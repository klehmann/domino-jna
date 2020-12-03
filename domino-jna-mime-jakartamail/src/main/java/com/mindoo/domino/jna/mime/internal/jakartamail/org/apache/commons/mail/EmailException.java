/**
 * ==========================================================================
 * Copyright (C) 2019-2020 HCL ( http://www.hcl.com/ )
 *                            All rights reserved.
 * ==========================================================================
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may
 * not use this file except in compliance with the License.  You may obtain a
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the  specific language  governing permissions  and limitations
 * under the License.
 * ==========================================================================
 */
package com.mindoo.domino.jna.mime.internal.jakartamail.org.apache.commons.mail;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Exception thrown when a checked error occurs in commons-email.
 * <p>
 * Adapted from FunctorException in Commons Collections.
 * <p>
 * Emulation support for nested exceptions has been removed in {@code Email 1.3},
 * supported by JDK &ge; 1.4.
 *
 * @since 1.0
 */
public class EmailException
        extends Exception
{
    /** Serializable version identifier. */
    private static final long serialVersionUID = 5550674499282474616L;

    /**
     * Constructs a new {@code EmailException} with no
     * detail message.
     */
    public EmailException()
    {
        super();
    }

    /**
     * Constructs a new {@code EmailException} with specified
     * detail message.
     *
     * @param msg  the error message.
     */
    public EmailException(final String msg)
    {
        super(msg);
    }

    /**
     * Constructs a new {@code EmailException} with specified
     * nested {@code Throwable} root cause.
     *
     * @param rootCause  the exception or error that caused this exception
     *                   to be thrown.
     */
    public EmailException(final Throwable rootCause)
    {
        super(rootCause);
    }

    /**
     * Constructs a new {@code EmailException} with specified
     * detail message and nested {@code Throwable} root cause.
     *
     * @param msg  the error message.
     * @param rootCause  the exception or error that caused this exception
     *                   to be thrown.
     */
    public EmailException(final String msg, final Throwable rootCause)
    {
        super(msg, rootCause);
    }

    /**
     * Prints the stack trace of this exception to the standard error stream.
     */
    @Override
    public void printStackTrace()
    {
        printStackTrace(System.err);
    }

    /**
     * Prints the stack trace of this exception to the specified stream.
     *
     * @param out  the {@code PrintStream} to use for output
     */
    @Override
    public void printStackTrace(final PrintStream out)
    {
        synchronized (out)
        {
            final PrintWriter pw = new PrintWriter(out, false);
            printStackTrace(pw);

            // Flush the PrintWriter before it's GC'ed.
            pw.flush();
        }
    }

    /**
     * Prints the stack trace of this exception to the specified writer.
     *
     * @param out  the {@code PrintWriter} to use for output
     */
    @Override
    public void printStackTrace(final PrintWriter out)
    {
        synchronized (out)
        {
            super.printStackTrace(out);
        }
    }
}
