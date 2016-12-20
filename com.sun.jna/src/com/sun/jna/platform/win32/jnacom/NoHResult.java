/*
 * Copyright 2011 Digital Rapids Corporation.
 */

package com.sun.jna.platform.win32.jnacom;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Annotation to indicate that the native definition doesn't return an HRESULT
 * @author casing.chu
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface NoHResult {

}
