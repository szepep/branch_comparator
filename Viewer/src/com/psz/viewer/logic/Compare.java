/*
 * Copyright (c) 1998-2015 ChemAxon Ltd. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * ChemAxon. You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the agreements
 * you entered into with ChemAxon.
 */
package com.psz.viewer.logic;

import com.psz.viewer.CompareTopComponent;
import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import org.openide.util.Exceptions;

/**
 *
 */
public abstract class Compare extends AbstractAction {

    private static Model srcModel = null;
    private static Model targetModel = null;

    public static Compare createSrcComare(Model model) {
        return new Src(model);
    }

    public static Compare createTargetComare(Model model) {
        return new Target(model);
    }

    private final Model model;

    public Compare(Model model) {
        this.model = model;
    }

    public Model getModel() {
        return model;
    }

    abstract void actionPerformedInternal();

    @Override
    public void actionPerformed(ActionEvent e) {
        actionPerformedInternal();
        if (Compare.targetModel != null && Compare.srcModel != null) {
            Model newModel = Model.create(Compare.srcModel.getSrcFile(), Compare.targetModel.getTargetFile());
            try {
                CompareTopComponent compareTopComponent = new CompareTopComponent(newModel);
                compareTopComponent.open();
                compareTopComponent.requestActive();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
            Compare.targetModel = null;
            Compare.srcModel = null;
        }
    }

    private static final class Src extends Compare {

        public Src(Model model) {
            super(model);
            putValue(NAME, "Compare as source");
        }

        @Override
        void actionPerformedInternal() {
            srcModel = getModel();
        }

    }

    private static final class Target extends Compare {

        public Target(Model model) {
            super(model);
            putValue(NAME, "Compare as target");
        }

        @Override
        void actionPerformedInternal() {
            targetModel = getModel();
        }

    }

}
