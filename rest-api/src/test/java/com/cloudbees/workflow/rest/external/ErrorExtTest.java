/*
 * The MIT License
 *
 * Copyright (c) 2015, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.workflow.rest.external;

import groovy.lang.MissingPropertyException;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.junit.Ignore;
import org.junit.Test;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class ErrorExtTest {

    /**
     * This is a test against an issue which is fixed in Groovy 3.0 (GROOVY-8936) - It might start
     * failing whenever groovy is updated to a version where this issue is fixed. See
     * {@link #testErrorExtNoTestNullPointerException()} for a variant of this test against a broken
     * local class.
     */
    @Test
    @Ignore("TODO: Fails in `new ErrorAction` as of https://github.com/jenkinsci/workflow-api-plugin/pull/110. That code would need to be changed to guard against null types for this test to pass.")
    public void testErrorExtNoGroovyNullPointerException() {
        Throwable throwable = new MissingPropertyException(null);
        ErrorAction errorAction = new ErrorAction(throwable);
        ErrorExt errorExt = ErrorExt.create(errorAction);
        assertThat(errorExt.getMessage(),
                        containsString("No message: NullPointerException"));
    }

    @Test
    public void testErrorExtNoTestNullPointerException() {
        Throwable throwable = new BrokenException();
        ErrorAction errorAction = new ErrorAction(throwable);
        ErrorExt errorExt = ErrorExt.create(errorAction);
        assertThat(errorExt.getMessage(),
                        containsString("No message: NullPointerException"));
    }

    private static class BrokenException extends Exception {
        private static final long serialVersionUID = 1L;

        @Override
        public String getMessage() {
            throw new NullPointerException("violating contract");
        }
    }
}
