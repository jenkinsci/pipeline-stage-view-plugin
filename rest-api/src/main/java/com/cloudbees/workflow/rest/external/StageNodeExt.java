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

import com.cloudbees.workflow.flownode.FlowNodeUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.NotExecutedNodeAction;
import org.jenkinsci.plugins.workflow.actions.StageAction;
import org.jenkinsci.plugins.workflow.actions.ThreadNameAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.graph.AtomNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.kohsuke.stapler.Stapler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class StageNodeExt extends FlowNodeExt {

    private List<AtomFlowNodeExt> stageFlowNodes;

    /** Bit of a hack but this lets us cache all the child nodes, not just the limited subset without adding to JSON responses */
    transient List<String> allChildNodeIds = new ArrayList<String>();

    // Limit the size of child nodes returned
    static final int MAX_CHILD_NODES = Integer.getInteger(StageNodeExt.class.getName()+".maxChildNodes", 100);

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<AtomFlowNodeExt> getStageFlowNodes() {
        return stageFlowNodes;
    }

    public void setStageFlowNodes(List<AtomFlowNodeExt> stageFlowNodes) {
        this.stageFlowNodes = stageFlowNodes;
    }

    public static boolean isStageNode(FlowNode node) {
        return (node.getAction(LabelAction.class) != null && node.getAction(ThreadNameAction.class) == null);
    }

    /** Return full list of child node IDs */
    @JsonIgnore // Just in case
    public List<String> getAllChildNodeIds() {
        return Collections.unmodifiableList(allChildNodeIds);
    }

    /** Hides child nodes, so we store a complete image but only return the minimal amount of data */

    protected static class ChildHidingWrapper extends StageNodeExt {
        protected StageNodeExt myNode;

        public FlowNodeLinks get_links() {return myNode.get_links();}
        public String getId() {return myNode.getId();}
        public String getName() {return myNode.getName();}
        public String getExecNode() {return myNode.getExecNode();}
        public StatusExt getStatus() {return myNode.getStatus();}
        public ErrorExt getError() {return myNode.getError();}
        public long getStartTimeMillis() {return myNode.getStartTimeMillis();}
        public long getDurationMillis() {return myNode.getDurationMillis();}
        public long getPauseDurationMillis() {return myNode.getPauseDurationMillis();}

        @Override
        public List<AtomFlowNodeExt> getStageFlowNodes() {
            return null;
        }

        protected ChildHidingWrapper(StageNodeExt stage) {
            this.myNode = stage;
        }
    }

    public StageNodeExt myWrapper() {
        return new ChildHidingWrapper(this);
    }

    @Override
    protected void calculateTimings(FlowNode node) {
        // StageNodeExt objects must have timing passed in
    }

    private void addStageAtomNodeData(List<FlowNode> atomFlowNodes) {
        List<AtomFlowNodeExt> newNodes = new ArrayList<AtomFlowNodeExt>();

        for (FlowNode stageNode : atomFlowNodes) {
            if (stageNode instanceof AtomNode) {
                // We're capping the number of nodes to prevent major performance issues
                if (newNodes.size() <= MAX_CHILD_NODES) {
                    AtomFlowNodeExt atomFlowNodeExt = AtomFlowNodeExt.create(stageNode);
                    if (atomFlowNodeExt.getStatus() == StatusExt.FAILED) {
                        this.setStatus(StatusExt.FAILED);
                    }
                    newNodes.add(atomFlowNodeExt);
                } // Just scan for status code
                if (FlowNodeUtil.getStatus(stageNode) == StatusExt.FAILED) {
                    this.setStatus(StatusExt.FAILED);
                }
            }
        }
        this.setStageFlowNodes(newNodes);
    }

}
