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

import org.jenkinsci.plugins.workflow.graph.FlowNode;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Simple node executor name cache.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class FlowNodeExecutorNameCache {

    private static final Map<FlowNode, String> execNodeNameCache = new WeakHashMap<FlowNode, String>();

    /**
     * Cache the name of the executing node on the workflow flow node.
     * @param execNodeName The name of the executing node.
     * @param flowNode The workflow flow.
     */
    public synchronized static void cache(@Nonnull String execNodeName, @Nonnull FlowNode flowNode) {
        execNodeNameCache.put(flowNode, execNodeName);
    }

    /**
     * Get the node executor name for the supplied {@link FlowNode} if one exists.
     * @param flowNode The workflow flow.
     * @return The node executor name for the supplied {@link FlowNode} if one exists,
     * otherwise {@code null}.
     */
    public synchronized static @CheckForNull String getExecNodeName(@Nonnull FlowNode flowNode) {
        return execNodeNameCache.get(flowNode);
    }
}
