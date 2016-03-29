package com.cloudbees.workflow.flownode;

import com.cloudbees.workflow.flownode.mock.FlowGraphBuilder;
import com.cloudbees.workflow.rest.external.ExecDuration;
import com.cloudbees.workflow.rest.external.StageNodeExt;
import com.cloudbees.workflow.rest.external.StatusExt;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.graph.FlowNode;

import org.junit.Ignore;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Created by svanoort on 3/18/16.
 */
@State(Scope.Thread)
@Ignore
public class Benchmarks {

    static final int LONG_RUN_NODES = 10000;

    static List<FlowNode> basis = initNodes();

    // Return a list of heads from a builder
    @Setup(Level.Iteration)
    public void setup() {
        Benchmarks.basis = initNodes();
    }

    public static List<FlowNode> initNodes() {
        FlowGraphBuilder graphBuilder = new FlowGraphBuilder();

        FlowGraphBuilder partiallyBuilt = graphBuilder.addNode("Start")
                .addNode("Prestage")
                .addStageNode("Build")
                .addInStageNode("Git");

        for (int i=0; i<20000; i++) {
            partiallyBuilt = partiallyBuilt.addInStageNode("BuildOp"+i);
        }

        partiallyBuilt.addInStageNode("Mvn - build")
                .addStageNode("Test")
                .addInStageNode("tests1").moveTo("Test") // Add all the test steps in parallel as children of the "Test" stage
                .addInStageNode("tests2").moveTo("Test")
                .addInStageNode("tests3")
                .addStageNode("Deploy", "tests1", "tests2", "tests3") // The parent steps are the test steps coz the executed in parallel
                .addInStageNode("Mvn - release");
        for (int i=0; i<LONG_RUN_NODES; i++) {
            partiallyBuilt = partiallyBuilt.addInStageNode("long"+i);
        }
        partiallyBuilt = partiallyBuilt.addNode("End");
        return partiallyBuilt.currentHeads;
    }

    @Benchmark
    public void oldStageScan() {
        /** This is a simplified version of the old scanning routines */
        List<FlowNode> stages = FlowNodeUtil.getStageNodes(basis.get(0).getExecution());
        List<ExecDuration> dur = new ArrayList<ExecDuration>();
        for (FlowNode f : stages) {
            StageNodeExt ext = StageNodeExt.create(f);
            dur.add(calculateTimings(f));
            List<FlowNode> childNodes = FlowNodeUtil.getStageNodes(f);
        }
    }

    @Benchmark
    public void justIterateStageScan() {
        /** Just run through nodes, checking stage node presence */
        FlowGraphWalker walker = new FlowGraphWalker(basis.get(0).getExecution());
        for (FlowNode f : walker) {
            boolean isStage = StageNodeExt.isStageNode(f);
            long startTime = TimingAction.getStartTime(f);
        }
    }

    protected ExecDuration calculateTimings(FlowNode node) {
        // Set the stage start time to be the start time of the first executed
        // node on the stage.
        FlowNode stageExecStartNode = FlowNodeUtil.getStageExecStartNode(node);
        ExecDuration execDuration = FlowNodeUtil.getStageExecDuration(node);
        return execDuration;
    }

    @Benchmark
    public void newStageScan() {
        FlowAnalyzer analyzer = new FlowAnalyzer();
        analyzer.reinitialize(basis);
        analyzer.setKeepChildren(true);
        analyzer.analyzeAll();
        for(FlowAnalyzer.StageEntry ent : analyzer.getStages()) {
            List children = ent.children;
            if (children.isEmpty()) {
                //throw new RuntimeException("No children found when expected");
            }
        }
    }

    @Test
    // Source http://stackoverflow.com/questions/30485856/how-to-run-jmh-from-inside-junit-tests
    public void launchBenchmark() throws Exception {

        Options opt = new OptionsBuilder()
                .include(this.getClass().getName() + ".*")
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.NANOSECONDS)
                .warmupIterations(3)
                .warmupTime(TimeValue.seconds(3))
                .measurementTime(TimeValue.seconds(3))
                .measurementIterations(3)
                .threads(1)
                .forks(2)
                .shouldFailOnError(true)
                .build();
        new Runner(opt).run();
    }
}
