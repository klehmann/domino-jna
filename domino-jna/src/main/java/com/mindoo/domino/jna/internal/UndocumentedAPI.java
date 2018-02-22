package com.mindoo.domino.jna.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})

/**
 * Annotation for methods that are not yet part of the public C API (but should be)
 */
public @interface UndocumentedAPI {

}