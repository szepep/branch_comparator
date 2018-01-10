/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.psz.viewer.logic;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

/**
 *
 * @author SzepePeter
 */
public class ComparatorNode extends AbstractNode {

    private final Model model;
    private Action[] actions;

    public static ComparatorNode create(File srcFile, File targetFile) {
        Model model = Model.create(srcFile, targetFile);
        ComparatorNode node = new ComparatorNode(model);
        node.setName("root");
        return node;
    }

    public static ComparatorNode create(String src, String target) {
        return create(new File(src), new File(target));
    }

    @Override
    public Action[] getActions(boolean context) {
        if (actions == null) {
            List<Action> actionsList = new ArrayList<Action>();
            switch (model.getType()) {
                case NEW:
                    actionsList.add(Compare.createTargetComare(model));
                    break;
                case DELETED:
                    actionsList.add(Compare.createSrcComare(model));
                    break;
                case MODIFIED:
                    actionsList.add(Compare.createSrcComare(model));
                    actionsList.add(Compare.createTargetComare(model));
                    break;
            }
            actions = actionsList.toArray(new Action[actionsList.size()]);
        }
        return actions;
    }

    private ComparatorNode(Model model) {
        super(model.getFile().isFile()
                ? Children.LEAF
                : Children.create(new ChildFactoryImpl(model), false));
        this.model = model;
    }

    public Model getModel() {
        return model;
    }

    @Override
    public String getHtmlDisplayName() {
        String name = model.getFile().getName();
        String color = "#000000";
        switch (model.getType()) {
            case NEW:
                color = "#33CC33";
                if (model.getSrcFile() != null) {
                    name = "<i>" + name + "</i>";
                }
                break;
            case DELETED:
                color = "#FF7272";
                if (model.getTargetFile() != null) {
                    name = "<i>" + name + "</i>";
                }
                break;
            case MODIFIED:
                color = "#000000";
                break;
        }
        return "<font color='" + color + "'>" + name + "</font>";
    }

    private static class ChildFactoryImpl extends ChildFactory<Model> {

        private final Model model;

        private ChildFactoryImpl(Model model) {
            this.model = model;
        }

        @Override
        protected boolean createKeys(List<Model> list) {
            List<Model> children = model.getChildren();
            list.addAll(children);
            return true;
        }

        @Override
        protected Node createNodeForKey(Model key) {
            Node result = new ComparatorNode(key);
            result.setDisplayName(key.toString());
            return result;
        }

    }

}
