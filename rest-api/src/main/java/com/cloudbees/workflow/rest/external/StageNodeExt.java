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
import com.fasterxml.jackson.annotation.JsonInclude;
import org.jenkinsci.plugins.workflow.actions.NotExecutedNodeAction;
import org.jenkinsci.plugins.workflow.actions.StageAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.graph.AtomNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class StageNodeExt extends FlowNodeExt {

    private List<AtomFlowNodeExt> stageFlowNodes;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<AtomFlowNodeExt> getStageFlowNodes() {
        return stageFlowNodes;
    }

    public void setStageFlowNodes(List<AtomFlowNodeExt> stageFlowNodes) {
        this.stageFlowNodes = stageFlowNodes;
    }

    public static boolean isStageNode(FlowNode node) {
        return (node.getAction(StageAction.class) != null);
    }

    public static StageNodeExt create(FlowNode node) {
        StageNodeExt stageNodeExt = new StageNodeExt();

        stageNodeExt.addBasicNodeData(node);

        // Use the last node in the stage to configure the stage status.
        FlowNode stageEndNode = FlowNodeUtil.getStageEndNode(node);
        if (NotExecutedNodeAction.isExecuted(stageEndNode)) {
            stageNodeExt.setStatus(StatusExt.valueOf(stageEndNode.getError()));
        } else {
            stageNodeExt.setStatus(StatusExt.NOT_EXECUTED);
        }

        return stageNodeExt;
    }

    public void addStageFlowNodes(FlowNode node) {
        List<FlowNode> stageFlowNodes = FlowNodeUtil.getStageNodes(node);
        addStageAtomNodeData(stageFlowNodes);
    }

    @Override
    protected void calculateTimings(FlowNode node) {
        // Set the stage start time to be the start time of the first executed
        // node on the stage.
        FlowNode stageExecStartNode = FlowNodeUtil.getStageExecStartNode(node);
        if (stageExecStartNode != null) {
            setStartTimeMillis(TimingAction.getStartTime(stageExecStartNode));
        }
        ExecDuration execDuration = FlowNodeUtil.getStageExecDuration(node);
        setDurationMillis(execDuration.getTotalDurationMillis());
        setPauseDurationMillis(execDuration.getPauseDurationMillis());
        setPauseDurationMillis(Math.min(getPauseDurationMillis(), getDurationMillis()));
    }

    private void addStageAtomNodeData(List<FlowNode> atomFlowNodes) {
        this.setStageFlowNodes(new ArrayList<AtomFlowNodeExt>());
        for (FlowNode stageNode : atomFlowNodes) {
            if (stageNode instanceof AtomNode) {
                AtomFlowNodeExt atomFlowNodeExt = AtomFlowNodeExt.create(stageNode);
                this.getStageFlowNodes().add(atomFlowNodeExt);
                if (atomFlowNodeExt.getStatus() == StatusExt.FAILED) {
                    this.setStatus(StatusExt.FAILED);
                }
            }
        }
    }

}
