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

import hudson.model.InvisibleAction;
import hudson.model.Queue;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.graph.FlowNode;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple action for caching pre processed/filtered/sorted node lists.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class FlowNodeListCacheAction extends InvisibleAction {

    private static final Logger LOGGER = Logger.getLogger(FlowNodeUtil.class.getName());

    public transient List<FlowNode> unsortedNodeList;
    public transient List<FlowNode> idSortedNodeList;

    public static FlowNodeListCacheAction get(FlowNode flowNode) {
        Queue.Executable execution;
        try {
            execution = flowNode.getExecution().getOwner().getExecutable();
        } catch (NullPointerException e) {
            LOGGER.log(Level.FINE, "NullPointerException getting Workflow Queue.Executable. Probably running in a mocked test. Returning a throwaway cache instance.");
            return new FlowNodeListCacheAction();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unexpected error getting Workflow Queue.Executable. Returning a throwaway cache instance.", e);
            return new FlowNodeListCacheAction();
        }

        if (execution instanceof Run) {
            Run run = (Run) execution;
            FlowNodeListCacheAction action;

            action = run.getAction(FlowNodeListCacheAction.class);
            if (action == null) {
                action = new FlowNodeListCacheAction();
                // Only store the cache if the run has completed.
                if (!run.isBuilding()) {
                    run.addAction(action);
                }
            }

            return action;
        } else if (execution == null) {
            LOGGER.log(Level.WARNING, "Unexpected 'null' Workflow Queue.Executable. Returning a throwaway cache instance.");
        } else {
            LOGGER.log(Level.WARNING, "Unexpected Workflow Queue.Executable type " + execution.getClass().getName() + ". Returning a throwaway cache instance.");
        }

        return new FlowNodeListCacheAction();
    }
}
