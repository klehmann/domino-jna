/*
 * Copyright 2010-2011 Digital Rapids Corporation.
 */

package com.sun.jna.platform.win32.jnacom;

/**
 *
 * @author scott.palmer
 */
public class ComException extends RuntimeException {
    private final int hresult;

    public ComException(String msg, int hresult) {
        super(msg);
        this.hresult = hresult;
    }

    public ComException(int hresult) {
        this("COM API returned 0x"+ Integer.toHexString(hresult), hresult);
    }

    public int getHRESULT() {
        return hresult;
    }
}
