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
package com.cloudbees.workflow.flownode;

import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link FlowNode} navigator.
 * <p>
 * Reverse depth-first navigation of a {@link FlowNode} graph (from a {@link FlowExecution}).
 * Iterates from the supplied {@link FlowNode}(s) back up through the parent {@link FlowNode}(s)
 * (i.e. in reverse order of node execution).
 * </p>
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@Restricted(NoExternalUse.class)
public class FlowNodeNavigator {

    private FlowNodeNavigationListener listener;
    private List<FlowNode> nodesAlreadyNavigated = new ArrayList<FlowNode>();

    public FlowNodeNavigator(FlowNodeNavigationListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("null 'listener' argument.");
        }
        this.listener = listener;
    }

    public void navigate(FlowNode node) {
        if (alreadyNavigated(node)) {
            return;
        }
        nodesAlreadyNavigated.add(node);
        listener.onNode(node);
        navigate(node.getParents());
    }

    public void navigate(List<FlowNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (FlowNode node: nodes) {
            navigate(node);
        }
    }

    private boolean alreadyNavigated(FlowNode node) {
        for (FlowNode alreadyNavigated : nodesAlreadyNavigated) {
            if (node == alreadyNavigated) {
                return true;
            }
        }
        return false;
    }
}
