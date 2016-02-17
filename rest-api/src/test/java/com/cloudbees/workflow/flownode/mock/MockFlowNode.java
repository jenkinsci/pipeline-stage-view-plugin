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
package com.cloudbees.workflow.flownode.mock;

import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.AtomNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;

/**
 * Mock FlowNode impl to mimic nodes other than {@link AtomNode}s.
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class MockFlowNode extends FlowNode {

    private static int iota;

    private final String name;

    protected static MockFlowNode newNode(FlowExecution exec, String name, FlowNode... parents) {
        String id = newIota();
        if (parents != null && parents.length > 0 && parents[0] != null) {
            return new MockFlowNode(exec, name, id, parents);
        } else {
            return new MockFlowNode(exec, name, id);
        }
    }

    protected MockFlowNode(FlowExecution exec, String name, String id, FlowNode... parents) {
        super(exec, id, parents);
        this.name = name;
    }

    @Override
    protected String getTypeDisplayName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    protected static String newIota() {
        return String.valueOf(iota++);
    }

    @Override
    public String toString() {
        return name;
    }
}
