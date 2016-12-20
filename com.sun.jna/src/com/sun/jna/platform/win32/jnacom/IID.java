/*
 * Copyright 2009-2011 Digital Rapids Corporation.
 */

package com.sun.jna.platform.win32.jnacom;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author scott.palmer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface IID {

    /**
     * GUID as a string like "<tt>{xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}</tt>".
     */
    String value();

}
