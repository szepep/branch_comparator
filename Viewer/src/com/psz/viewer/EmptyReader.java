/*
 * Copyright (c) 1998-2015 ChemAxon Ltd. All Rights Reserved.
 * 
 * This software is the confidential and proprietary information of
 * ChemAxon. You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the agreements
 * you entered into with ChemAxon.
 */

package com.psz.viewer;

import java.io.IOException;
import java.io.Reader;

/**
 *
 */
class EmptyReader extends Reader {

    public EmptyReader() {
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return -1;
    }

    @Override
    public void close() throws IOException {
    }

}
