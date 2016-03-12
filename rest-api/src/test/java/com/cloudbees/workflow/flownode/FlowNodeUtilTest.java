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

import com.cloudbees.workflow.flownode.mock.FlowGraphBuilder;
import org.jenkinsci.plugins.workflow.actions.NotExecutedNodeAction;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class FlowNodeUtilTest {

    @Test
    public void test_node_sorting() {
        FlowGraphBuilder graphBuilder = new FlowGraphBuilder();

        graphBuilder.addNode("1")
                .addStageNode("2")
                .addInStageNode("3")
                .addInStageNode("4")
                .addStageNode("5")
                .addInStageNode("6")
                .addInStageNode("7")
                .addStageNode("8")
                .addInStageNode("9")
                .addInStageNode("10")
                .addNode("11")
        ;

        ArrayList<FlowNode> nodeList = new ArrayList<FlowNode>(graphBuilder.nodeMap.values());
        FlowNodeUtil.sortNodesById(nodeList);

        Assert.assertEquals(
                "[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]",
                nodeList.toString());
    }

    @Test
    public void test_getChildNodes() {
        FlowGraphBuilder graphBuilder = new FlowGraphBuilder();

        graphBuilder.addNode("Start")
                .addStageNode("Build")
                .addInStageNode("Git")
                .addInStageNode("Mvn - build")
                .addStageNode("Test")
                .addInStageNode("tests1").moveTo("Test") // Add all the test steps in parallel as children of the "Test" stage
                .addInStageNode("tests2").moveTo("Test")
                .addInStageNode("tests3")
                .addStageNode("Deploy", "tests1", "tests2", "tests3") // The parent steps are the test steps coz the executed in parallel
                .addInStageNode("Mvn - release")
                .addInStageNode("Notify XYZ")
                .addNode("End")
        ;

        Assert.assertEquals("[Git]", FlowNodeUtil.getChildNodes(graphBuilder.getNode("Build")).toString());
        Assert.assertEquals("[Mvn - build]", FlowNodeUtil.getChildNodes(graphBuilder.getNode("Git")).toString());
        Assert.assertEquals("[Test]", FlowNodeUtil.getChildNodes(graphBuilder.getNode("Mvn - build")).toString());
        // "Test" has 3 kids ... all tests kicked off in parallel
        Assert.assertEquals("[tests1, tests2, tests3]", FlowNodeUtil.getChildNodes(graphBuilder.getNode("Test")).toString());
        // The test steps all have "Deploy" as their child... I think this is correct!! TODO
        Assert.assertEquals("[Deploy]", FlowNodeUtil.getChildNodes(graphBuilder.getNode("tests1")).toString());
        Assert.assertEquals("[Deploy]", FlowNodeUtil.getChildNodes(graphBuilder.getNode("tests2")).toString());
        Assert.assertEquals("[Deploy]", FlowNodeUtil.getChildNodes(graphBuilder.getNode("tests3")).toString());
        Assert.assertEquals("[Mvn - release]", FlowNodeUtil.getChildNodes(graphBuilder.getNode("Deploy")).toString());
    }

    @Test
    public void test_getStageNodes() {
        FlowGraphBuilder graphBuilder = new FlowGraphBuilder();

        graphBuilder.addNode("Start")
                .addStageNode("Build")
                .addInStageNode("Git")
                .addInStageNode("Mvn - build")
                .addStageNode("Test")
                .addInStageNode("Mvn - tests")
                .addInStageNode("Mvn - more tests")
                .addStageNode("Deploy")
                .addInStageNode("Mvn - release")
                .addInStageNode("Notify XYZ")
                .addNode("End")
        ;

        FlowNode n = graphBuilder.getNode("Build");
        List<FlowNode> buildNodes = FlowNodeUtil.getStageNodes(n);
        Assert.assertEquals("[Git, Mvn - build]", buildNodes.toString());

        List<FlowNode> testNodes = FlowNodeUtil.getStageNodes(graphBuilder.getNode("Test"));
        Assert.assertEquals("[Mvn - tests, Mvn - more tests]", testNodes.toString());

        List<FlowNode> deployNodes = FlowNodeUtil.getStageNodes(graphBuilder.getNode("Deploy"));
        Assert.assertEquals("[Mvn - release, Notify XYZ, End]", deployNodes.toString());
    }

    @Test
    public void test_getNodeExecDuration() {
        FlowGraphBuilder graphBuilder = new FlowGraphBuilder();

        graphBuilder.addNode("Start")
                .addStageNode("Build")
                .addInStageNode("Git", 4000L)
                .addInStageNode("Mvn - build", 100000L)
                .addStageNode("Test")
                .addInStageNode("Mvn - tests", 500000L)
                .addInStageNode("Mvn - more tests", 100000L)
                .addInStageNode("Mvn - and more tests", 20000L)
                .addStageNode("Deploy")
                .addInStageNode("Mvn - release")
                .addInStageNode("Notify XYZ")
                .addNode("End")
        ;

        Assert.assertEquals(5L, FlowNodeUtil.getNodeExecDuration(graphBuilder.getNode("Build")));
        Assert.assertEquals(4000L, FlowNodeUtil.getNodeExecDuration(graphBuilder.getNode("Git")));
        Assert.assertEquals(500000L, FlowNodeUtil.getNodeExecDuration(graphBuilder.getNode("Mvn - tests")));
    }

    @Test
    public void test_getStageExecDuration() {
        FlowGraphBuilder graphBuilder = new FlowGraphBuilder();

        graphBuilder.addNode("Start")
                .addStageNode("Build")
                .addInStageNode("Git", 4000L)
                .addInStageNode("Mvn - build", 100000L)
                .addStageNode("Test")
                .addInStageNode("Mvn - tests", 500000L)
                .addInStageNode("Mvn - more tests", 100000L)
                .addInStageNode("Mvn - and more tests", 20000L)
                .addStageNode("Deploy")
                .addInStageNode("Mvn - release")
                .addInStageNode("Notify XYZ")
                .addNode("End")
        ;

        Assert.assertEquals(104005L, FlowNodeUtil.getStageExecDuration(graphBuilder.getNode("Build")).getTotalDurationMillis());
        Assert.assertEquals(620005L, FlowNodeUtil.getStageExecDuration(graphBuilder.getNode("Test")).getTotalDurationMillis());
        Assert.assertEquals(20005L, FlowNodeUtil.getStageExecDuration(graphBuilder.getNode("Deploy")).getTotalDurationMillis());
    }

    @Test
    public void test_with_non_executed_action() {
        FlowGraphBuilder graphBuilder = new FlowGraphBuilder();

        graphBuilder.addNode("Start")
                .addStageNode("Build").addAction(new NotExecutedNodeAction())
                .addInStageNode("Git", 4000L).addAction(new NotExecutedNodeAction())
                .addInStageNode("Mvn - build", 100000L).addAction(new NotExecutedNodeAction())
                .addStageNode("Test").addAction(new NotExecutedNodeAction())
                .addInStageNode("Mvn - tests", 500000L).addAction(new NotExecutedNodeAction())
                .addInStageNode("checkpoint - pre deploy").addAction(new NotExecutedNodeAction())
                .addInStageNode("Mvn - more tests", 100000L)
                .addInStageNode("Mvn - and more tests", 20000L)
                .addStageNode("Deploy")
                .addInStageNode("Mvn - release")
                .addInStageNode("Notify XYZ")
                .addNode("End")
        ;

        // Build stage should not have any executed nodes
        Assert.assertEquals(null, FlowNodeUtil.getStageExecStartNode(graphBuilder.getNode("Build")));

        // Test stage only starts at 'Mvn - more tests'
        FlowNode testStageStart = FlowNodeUtil.getStageExecStartNode(graphBuilder.getNode("Test"));
        Assert.assertEquals("Mvn - more tests", testStageStart.getDisplayName());

        // Next stage should be the Deploy stage.
        FlowNode nextStageNode = FlowNodeUtil.getNextStageNode(graphBuilder.getNode("Test"));
        Assert.assertEquals("Deploy", nextStageNode.getDisplayName());

        // Build stage duration should be zero
        Assert.assertEquals(0L, FlowNodeUtil.getStageExecDuration(graphBuilder.getNode("Build")).getTotalDurationMillis());

        // Test stage duration should not include the 'Mvn -tests' node duration
        Assert.assertEquals(120000L, FlowNodeUtil.getStageExecDuration(graphBuilder.getNode("Test")).getTotalDurationMillis());
        Assert.assertEquals(20005L, FlowNodeUtil.getStageExecDuration(graphBuilder.getNode("Deploy")).getTotalDurationMillis());
    }
}
