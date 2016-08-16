package com.cloudbees.workflow.rest.external;

import com.google.common.collect.Iterables;
import org.jenkinsci.plugins.workflow.actions.NotExecutedNodeAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.graph.BlockEndNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graphanalysis.ForkScanner;
import org.jenkinsci.plugins.workflow.graphanalysis.MemoryFlowChunk;
import org.jenkinsci.plugins.workflow.graphanalysis.StandardChunkVisitor;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.pipelinegraphanalysis.GenericStatus;
import org.jenkinsci.plugins.workflow.pipelinegraphanalysis.StatusAndTiming;
import org.jenkinsci.plugins.workflow.pipelinegraphanalysis.TimingInfo;
import org.jenkinsci.plugins.workflow.support.actions.PauseAction;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Couples to the new analysis APIs to collect stages for processing
 * Created by @author <samvanoort@gmail.com>Sam Van Oort</samvanoort@gmail.com>
 */
public class ChunkVisitor extends StandardChunkVisitor {
    ArrayDeque<StageNodeExt> stages = new ArrayDeque<StageNodeExt>();
//    FlowNode firstExecuted = null;
    ArrayDeque<AtomFlowNodeExt> stageContents = new ArrayDeque<AtomFlowNodeExt>();
    WorkflowRun run;

    public ChunkVisitor(@Nonnull WorkflowRun run) {
        this.run = run;
    }

    public Collection<StageNodeExt> getStages() {
        return stages;
    }

    public static AtomFlowNodeExt makeAtomNode(@Nonnull WorkflowRun run, @CheckForNull FlowNode beforeNode, @Nonnull FlowNode node, @CheckForNull FlowNode next) {
        long pause = PauseAction.getPauseDuration(node);
        TimingInfo times = StatusAndTiming.computeChunkTiming(run, pause, beforeNode, node, node, next); // TODO pipeline graph analysis adds this to TimingInfo
        ExecDuration dur = (times == null) ? new ExecDuration() : new ExecDuration(times);

        GenericStatus status = StatusAndTiming.computeChunkStatus(run, beforeNode, node, node, next);
        if (status == null) {
            status = GenericStatus.NOT_EXECUTED;
        }

        // TODO pipeline graph analysis gets most of the metadata for the chunk in 1 pass
        AtomFlowNodeExt output = AtomFlowNodeExt.create(node, "", dur, TimingAction.getStartTime(node), StatusExt.fromGenericStatus(status), node.getError());
        return output;
    }

    @Override
    /** Do the final computations to materialize the stage */
    protected void handleChunkDone(@Nonnull MemoryFlowChunk chunk) {
        StageNodeExt stageExt = new StageNodeExt();
        TimingInfo times = StatusAndTiming.computeChunkTiming(run, chunk.getPauseTimeMillis(), chunk);
        ExecDuration dur = (times == null) ? new ExecDuration() : new ExecDuration(times);

        GenericStatus status = StatusAndTiming.computeChunkStatus(run, chunk);
        if (status == null) {
            status = GenericStatus.NOT_EXECUTED;
        }

        // TODO pipeline graph analysis gets most of the metadata for the chunk in 1 pass

        stageExt.addBasicNodeData(chunk.getFirstNode(), "", dur, TimingAction.getStartTime(chunk.getFirstNode()), StatusExt.fromGenericStatus(status), chunk.getLastNode().getError());
        stageExt.setStartTimeMillis(TimingAction.getStartTime(chunk.getFirstNode()));  // TODO pipeline graph analysis adds this to TimingInfo

        int childNodeLength = Math.min(StageNodeExt.MAX_CHILD_NODES, stageContents.size());
        ArrayList<AtomFlowNodeExt> internals = new ArrayList<AtomFlowNodeExt>(childNodeLength);
        Iterables.addAll(internals, Iterables.limit(stageContents, StageNodeExt.MAX_CHILD_NODES));
        stageExt.setStageFlowNodes(internals);

        this.stages.push(stageExt);
    }

    @Override
    protected void resetChunk(@Nonnull MemoryFlowChunk chunk) {
        super.resetChunk(chunk);
//        firstExecuted = null;
    }


    /** Called when hitting the end of a block (determined by the chunkEndPredicate) */
    public void chunkEnd(@Nonnull FlowNode endNode, @CheckForNull FlowNode afterBlock, @Nonnull ForkScanner scanner) {
        super.chunkEnd(endNode, afterBlock, scanner);

        // Reset the stage internal state to start here
        stageContents.clear();
        chunk.setPauseTimeMillis(0);

        // if we're using marker-based (and not block-scoped) stages, add the last node as part of its contents
        if (!(endNode instanceof BlockEndNode)) {
            atomNode(null, endNode, afterBlock, scanner);
        }
    }

    public void atomNode(@CheckForNull FlowNode before, @Nonnull FlowNode atomNode, @CheckForNull FlowNode after, @Nonnull ForkScanner scan) {
        /*if (!NotExecutedNodeAction.isExecuted(atomNode)) {
            firstExecuted = atomNode;  // See if we need this
        }*/
        long pause = PauseAction.getPauseDuration(atomNode);
        chunk.setPauseTimeMillis(chunk.getPauseTimeMillis()+pause);

        // TODO this is rather inefficient, we should optimize to use a circular buffer or ArrayList with limited size
        // And then only create the node container objects when we hit the start (doing timing ETC at that point)
        stageContents.push(makeAtomNode(run, before, atomNode, after));
    }
}
