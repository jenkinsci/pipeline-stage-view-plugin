package com.cloudbees.workflow.flownode;

import com.cloudbees.workflow.flownode.mock.FlowGraphBuilder;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.junit.Test;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Content by svanoort
 */
public class FlowAnalyzerTest {

    @Test
    /** Basic handling of node walking with parallel branches allowed */
    public void testBasics() {
        FlowGraphBuilder graphBuilder = new FlowGraphBuilder();

        graphBuilder.addNode("Start")
                .addNode("Prestage")
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

        FlowAnalyzer analyzer = new FlowAnalyzer(graphBuilder.currentHeads);
        analyzer.analyzeAll();

        Assert.assertTrue(analyzer.q.isEmpty());
        List<FlowAnalyzer.FlowSegment> stages = analyzer.stages;
        Assert.assertEquals(4, stages.size());

        // Headless stage before our first stage block
        FlowAnalyzer.FlowSegment st = stages.get(0);
        Assert.assertNull(st.stageNode);
        Assert.assertEquals(2, st.children.size());
        Assert.assertFalse(st.hasForks);
        Assert.assertEquals(graphBuilder.getNode("Start"), st.firstChild);
        Assert.assertEquals(graphBuilder.getNode("Prestage"), st.lastChild);

        // Check the individual stages
        st = stages.get(1);
        Assert.assertEquals(graphBuilder.getNode("Build"), st.stageNode);
        Assert.assertFalse(st.hasForks);
        Assert.assertEquals(2, st.nodeCount);
        Assert.assertEquals(graphBuilder.getNode("Git"), st.firstChild);
        Assert.assertEquals(graphBuilder.getNode("Git"), st.firstExecutedNode);
        Assert.assertEquals(graphBuilder.getNode("Mvn - build"), st.lastChild);

        st = stages.get(2);
        Assert.assertEquals(3, st.nodeCount);
//        Assert.assertTrue(st.hasForks); // FIXME
        Assert.assertEquals(graphBuilder.getNode("Test"), st.stageNode);

        // Candidate nodes can be be first OR last because they are parallel
        List<FlowNode> candidates = new ArrayList<FlowNode>();
        candidates.add(graphBuilder.getNode("tests1"));
        candidates.add(graphBuilder.getNode("tests2"));
        candidates.add(graphBuilder.getNode("tests3"));
        Assert.assertTrue(candidates.contains(st.firstChild));
        Assert.assertTrue(candidates.contains(st.lastChild));
        Assert.assertTrue(candidates.contains(st.firstExecutedNode));


        st = stages.get(3);
        Assert.assertEquals(2, st.nodeCount);
        Assert.assertFalse(st.hasForks);
        Assert.assertEquals(graphBuilder.getNode("Mvn - release"), st.firstChild);
        Assert.assertEquals(graphBuilder.getNode("Mvn - release"), st.firstExecutedNode);
        Assert.assertEquals(graphBuilder.getNode("Notify XYZ"), st.lastChild);
    }

    @Test
    public void testParallel() {

    }

    @Test
    public void testStatusEvaluation() {
        // NotExecuted
        // Fail
        // Success

    }

}
