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
package com.cloudbees.workflow.rest.endpoints.flownode;

import com.cloudbees.workflow.rest.external.AtomFlowNodeExt;
import com.cloudbees.workflow.rest.external.FlowNodeExt;
import com.cloudbees.workflow.rest.external.RunExt;
import com.cloudbees.workflow.rest.external.StageNodeExt;
import com.cloudbees.workflow.rest.endpoints.FlowNodeAPI;
import hudson.model.Queue;
import org.jenkinsci.plugins.workflow.graph.AtomNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.io.IOException;

/**
 * {@link FlowNode} "describe" endpoint.
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class Describe {

    public static String getUrl(FlowNode node) {
        return FlowNodeAPI.getUrl(node) + "/describe";
    }

    /**
     * Fetch API results for a node or stage
     * @param node Node to fetch, or start node for a stage
     * @return Node API response object (may be a stage)
     * @throws IOException In probably very rare transitory situations where a WorkflowRun doesn't have a queueExecutable
     */
    public static FlowNodeExt get(FlowNode node) throws IOException {
        if (StageNodeExt.isStageNode(node)) {

            // Digest the WorkflowRun to get the stages, using cache if possible.
            // Future optimization for big runs: use the blackLists in the ForkScanner only digest until the
            // Node before the stage
            Queue.Executable exec = node.getExecution().getOwner().getExecutable();
            if (exec instanceof WorkflowRun) {
                WorkflowRun run = (WorkflowRun)exec;
                RunExt runExt = RunExt.create(run);
                for (StageNodeExt st : runExt.getStages()) {
                    if (st.getId().equals(node.getId())) {
                        return st;
                    }
                }
            }

            // This would mean that the node is a stage node but not in its own run...
            return null;
        } else if (node instanceof AtomNode) {
            return AtomFlowNodeExt.create(node);
        } else {
            return FlowNodeExt.create(node);
        }
    }
}
