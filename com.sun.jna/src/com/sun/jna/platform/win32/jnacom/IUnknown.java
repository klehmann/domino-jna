/*
 * Copyright 2009-2011 Digital Rapids Corporation.
 */

package com.sun.jna.platform.win32.jnacom;

/**
 * Base interface for COM interfaces.  Provides IUnknown and a dispose() method.
 * @author scott.palmer
 */
@IID("{00000000-0000-0000-C000-000000000046}")
public interface IUnknown {
    /** dispose is like release, but it forces the internal native pointer to NULL. */
    void dispose();
    @VTID(0)
    <T extends IUnknown> T queryInterface(Class<? extends IUnknown> comInterface);
    @VTID(1)
    int addRef();
    @VTID(2)
    int release();
}
