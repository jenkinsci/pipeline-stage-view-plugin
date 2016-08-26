/*
 * The MIT License
 *
 * Copyright (c) 2013-2016, CloudBees, Inc.
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

import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.pipelinegraphanalysis.GenericStatus;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public enum StatusExt {
    NOT_EXECUTED,
    ABORTED,
    SUCCESS,
    IN_PROGRESS,
    PAUSED_PENDING_INPUT,
    FAILED,
    UNSTABLE;  // Custom values

    public static StatusExt valueOf(ErrorAction errorAction) {
        if (errorAction == null) {
            return StatusExt.SUCCESS;
        } else {
            return valueOf(errorAction.getError());
        }
    }

    public static StatusExt valueOf(Throwable t) {
        if (t instanceof FlowInterruptedException) {
            return StatusExt.ABORTED;
        } else {
            return StatusExt.FAILED;
        }
    }

    public static StatusExt fromGenericStatus(@CheckForNull GenericStatus st) {
        if (st == null) {
            return StatusExt.NOT_EXECUTED;
        }
        switch (st) {
            case PAUSED_PENDING_INPUT: return StatusExt.PAUSED_PENDING_INPUT;
            case ABORTED: return StatusExt.ABORTED;
            case FAILURE: return StatusExt.FAILED;
            case IN_PROGRESS: return StatusExt.IN_PROGRESS;
            case UNSTABLE: return StatusExt.UNSTABLE;
            case SUCCESS: return StatusExt.SUCCESS;
            case NOT_EXECUTED: return StatusExt.NOT_EXECUTED;
            default:
                throw new IllegalStateException("Forbidden GenericStatus: "+st);
        }
    }
}
