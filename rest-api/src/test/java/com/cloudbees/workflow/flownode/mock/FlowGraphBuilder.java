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

import hudson.model.Action;
import org.jenkinsci.plugins.workflow.actions.StageAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.junit.Assert;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class FlowGraphBuilder {

    public static final long DEFAULT_DURATION = 10000L;

    public Map<String, FlowNode> nodeMap = new HashMap<String, FlowNode>();
    public List<FlowNode> currentHeads = new ArrayList<FlowNode>();
    public FlowExecution flowExecution;
    public FlowNode currentNode;
    public long theTime = 1412600000000L;

    public FlowGraphBuilder() {
        flowExecution = Mockito.mock(FlowExecution.class);
        // Would like to mock the WorkflowRun and WorkflowJob but not obviously possible because
        // they are final => can't Mockito or extend.
        Mockito.when(flowExecution.getCurrentHeads()).thenReturn(currentHeads);
    }

    public FlowGraphBuilder addNode(String name) {
        FlowNode newNode = MockFlowNode.newNode(flowExecution, name, currentNode);
        _addNode(name, newNode, DEFAULT_DURATION);
        return this;
    }

    public FlowGraphBuilder addStageNode(final String name, String... parentNames) {
        FlowNode newNode = MockAtomFlowNode.newNode(flowExecution, name, getParents(parentNames));
        newNode.addAction(new StageAction() {
            @Override
            public String getStageName() {
                return "Stage-" + name;
            }
            @Override
            public String getIconFileName() {
                return null;
            }
            @Override
            public String getDisplayName() {
                return getStageName();
            }
            @Override
            public String getUrlName() {
                return null;
            }
        });
        _addNode(name, newNode, 5L);
        return this;
    }

    public FlowGraphBuilder addInStageNode(String id, String... parentNames) {
        FlowNode newNode = MockAtomFlowNode.newNode(flowExecution, id, getParents(parentNames));
        _addNode(id, newNode, DEFAULT_DURATION);
        return this;
    }

    public FlowGraphBuilder addInStageNode(String id, long duration, String... parentNames) {
        FlowNode newNode = MockAtomFlowNode.newNode(flowExecution, id, getParents(parentNames));
        _addNode(id, newNode, duration);
        return this;
    }

    public FlowGraphBuilder addAction(Action action) {
        if (currentNode == null) {
            Assert.fail("There's no currentNode. Unable to add an action.");
        }
        currentNode.addAction(action);
        return this;
    }

    private FlowNode _addNode(String name, FlowNode newNode, long duration) {
        if (nodeMap.containsKey(name)) {
            Assert.fail("Node with name " + name + " already exists.");
        }

        final long nodeStartTime = theTime;
        newNode.addAction(new TimingAction() {
            @Override
            public long getStartTime() {
                return nodeStartTime;
            }
        });
        theTime += duration;

        nodeMap.put(name, newNode);
        currentNode = newNode;
        setCurrentHeads(name);
        return newNode;
    }

    public FlowGraphBuilder moveTo(String toName) {
        currentNode = getNode(toName);
        return this;
    }

    public void setCurrentHeads(String... nodeNames) {
        currentHeads.clear();
        for (String nodeName : nodeNames) {
            currentHeads.add(getNode(nodeName));
        }
    }

    public FlowNode getNode(String nodeName) {
        FlowNode node = nodeMap.get(nodeName);
        if (node == null) {
            Assert.fail("Unknown node " + nodeName);
        }
        return node;
    }

    private List<FlowNode> getParents(String... parentNames) {
        List<FlowNode> parents = new ArrayList<FlowNode>();
        for (String parentName : parentNames) {
            parents.add(getNode(parentName));
        }
        // default is the currentNode
        if (parents.isEmpty() && currentNode != null) {
            parents.add(currentNode);
        }
        return parents;
    }
}
