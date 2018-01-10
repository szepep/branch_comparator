/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.psz.viewer.logic;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.codec.digest.DigestUtils;
import org.openide.util.Exceptions;

/**
 *
 * @author SzepePeter
 */
public class Model implements Comparable<Model> {

    public enum Type {

        MODIFIED,
        NEW,
        DELETED;
    }

    private static final Map<String, Model> NOT_PAIRED = new HashMap<String, Model>();

    private static final FileFilter FILTER = new FileFilter() {

        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory() || pathname.getName().endsWith(".java");
        }
    };

    public static Model create(File srcFile, File targetFile) {
        NOT_PAIRED.clear();
        return new Model(srcFile, targetFile);
    }

    private boolean changed = false;
    private File srcFile;
    private File targetFile;
    private final String srcRoot;
    private final String targetRoot;
    private final List<Model> children = new ArrayList<Model>();
    private final Type type;

    private Model(File srcFile, File targetFile) {
        this(srcFile.getAbsolutePath(), srcFile, targetFile.getAbsolutePath(), targetFile);
    }

    private Model(String srcRoot, File srcFile, String targetRoot, File targetFile) {
        this.srcFile = srcFile;
        this.srcRoot = srcRoot;
        this.targetFile = targetFile;
        this.targetRoot = targetRoot;

        if (srcFile == null) {
            type = Type.NEW;
        } else if (targetFile == null) {
            type = Type.DELETED;
        } else {
            type = Type.MODIFIED;
        }

        processRecursively();
    }

    public Type getType() {
        return type;
    }

    private void processRecursively() {
        if ((srcFile != null && srcFile.isDirectory()) || (targetFile != null && targetFile.isDirectory())) {
            processDirectory();
        } else {
            processFile();
        }
    }

    protected void processDirectory() {
        String path = getFile().getAbsolutePath().replaceAll("\\\\", "/");
        if (path.matches(".+(test/unit|build/classes-generated|tmp/netbeans)")) {
            return;
        }

        Map<String, File> srcFiles = createMapping(srcFile, srcRoot);
        Map<String, File> targetFiles = createMapping(targetFile, targetRoot);
        Set<String> common = intersection(srcFiles.keySet(), targetFiles.keySet());
        for (String name : common) {
            Model model = new Model(srcRoot, srcFiles.get(name), targetRoot, targetFiles.get(name));
            if (model.changed) {
                children.add(model);
            }
        }

        Set<String> inSrc = new HashSet<String>(srcFiles.keySet());
        inSrc.removeAll(common);
        for (String name : inSrc) {
            Model model = new Model(srcRoot, srcFiles.get(name), targetRoot, null);
            if (model.changed) {
                children.add(model);
            }
        }

        Set<String> inTarget = new HashSet<String>(targetFiles.keySet());
        inTarget.removeAll(common);
        for (String name : inTarget) {
            Model model = new Model(srcRoot, null, targetRoot, targetFiles.get(name));
            if (model.changed) {
                children.add(model);
            }
        }

        Collections.sort(children);
        changed = !children.isEmpty();
    }

    public List<Model> getChildren() {
        return children;
    }

    private <T> Set<T> intersection(Set<T> a, Set<T> b) {
        Set<T> result = new HashSet<T>(a);
        Set<T> notInB = new HashSet<T>(a);
        notInB.removeAll(b);
        result.removeAll(notInB);
        return result;
    }

    private Set<File> getFiles(File file) {
        return file == null || !file.isDirectory()
                ? Collections.<File>emptySet()
                : new HashSet<File>(Arrays.asList(file.listFiles(FILTER)));
    }

    private Map<String, File> createMapping(File file, String root) {
        Map<String, File> result = new HashMap<String, File>();
        for (File f : getFiles(file)) {
            result.put(f.getAbsolutePath().substring(root.length()), f);
        }
        return result;
    }

    private void processFile() {
        changed = srcFile == null || targetFile == null;
        if (changed) {
            if (!findMovedAndPair()) {
                NOT_PAIRED.put(getFile().getName(), this);
            }
        } else {
            changed = compareByDiggest();
            if (changed) {
                changed = compareLines();
            }
        }
    }

    protected boolean compareByDiggest() {
        try {
            return !createDiggest(srcFile).equals(createDiggest(targetFile));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }

    protected boolean compareLines() {
        List<String> srcLines = fileToLines(srcFile);
        List<String> targetLines = fileToLines(targetFile);
        Patch diff = DiffUtils.diff(srcLines, targetLines);
        for (Delta delta : diff.getDeltas()) {
            if (!justImport(delta.getOriginal().getLines()) || !justImport(delta.getRevised().getLines())) {
                return true;
            }
        }
        return false;
    }

    protected boolean justImport(List<?> lines) {
        for (Object line : lines) {
            String l = (String) line;
            boolean justImport = l.trim().startsWith("import") || l.trim().isEmpty();
            if (!justImport) {
                return false;
            }
        }
        return true;
    }

    private List<String> fileToLines(File file) {
        List<String> lines = new LinkedList<String>();
        String line = "";
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            while ((line = in.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private String createDiggest(File file) throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(file);
        String md5 = DigestUtils.md5Hex(fis);
        fis.close();
        return md5;
    }

    protected boolean findMovedAndPair() {
        Model maybeMoved = NOT_PAIRED.get(getName());
        if (maybeMoved != null && pairWithThis(maybeMoved)) {
            NOT_PAIRED.remove(getName());
            return true;
        }
        return false;
    }

    private String getName() {
        return srcFile == null ? targetFile.getName() : srcFile.getName();
    }

    private boolean pairWithThis(Model maybeMoved) {
        if (srcFile == null && maybeMoved.srcFile != null) {
            srcFile = maybeMoved.srcFile;
            maybeMoved.targetFile = targetFile;
            return true;
        } else if (targetFile == null && maybeMoved.targetFile != null) {
            targetFile = maybeMoved.targetFile;
            maybeMoved.srcFile = srcFile;
            return true;
        }
        return false;
    }

    public File getSrcFile() {
        return srcFile;
    }

    public File getTargetFile() {
        return targetFile;
    }

    @Override
    public String toString() {
        return getName();
    }

    public File getFile() {
        return srcFile == null ? targetFile : srcFile;
    }

    @Override
    public int compareTo(Model o) {
        return getFile().getName().compareTo(o.getFile().getName());
    }

}
