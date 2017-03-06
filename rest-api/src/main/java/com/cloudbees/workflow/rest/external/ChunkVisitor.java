package com.cloudbees.workflow.rest.external;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.NotExecutedNodeAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.graph.AtomNode;
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
 * Couples to the new analysis APIs to collect stages for processing.
 * This is where all the interesting parts happen.
 * @author Sam Van Oort
 */
public class ChunkVisitor extends StandardChunkVisitor {
    ArrayDeque<StageNodeExt> stages = new ArrayDeque<StageNodeExt>();
    FlowNode firstExecuted = null;
    ArrayDeque<AtomFlowNodeExt> stageContents = new ArrayDeque<AtomFlowNodeExt>();
    WorkflowRun run;
    ArrayList<String> stageNodeIds = new ArrayList<String>();
    boolean isLastChunk = true;

    public ChunkVisitor(@Nonnull WorkflowRun run) {
        this.run = run;
    }

    public Collection<StageNodeExt> getStages() {
        return stages;
    }

    public static AtomFlowNodeExt makeAtomNode(@Nonnull WorkflowRun run, @CheckForNull FlowNode beforeNode, @Nonnull FlowNode node, @CheckForNull FlowNode next) {
        if (!(node instanceof AtomNode)) {
            return null;
        }

        long pause = PauseAction.getPauseDuration(node);
        TimingInfo times = StatusAndTiming.computeChunkTiming(run, pause, node, node, next);
        ExecDuration dur = (times == null) ? new ExecDuration() : new ExecDuration(times);

        GenericStatus status = StatusAndTiming.computeChunkStatus(run, beforeNode, node, node, next);
        if (status == null) {
            status = GenericStatus.NOT_EXECUTED;
        }
        ErrorAction err = node.getError();
        if (status == GenericStatus.FAILURE && err == null) {
            // Needed for FlowEndNode, since an ErrorAction isn't generated
            // When build result is set directly
            err = new ErrorAction(new Throwable("Build marked as a failure, final stage will fail."));
        }

        AtomFlowNodeExt output = AtomFlowNodeExt.create(node, "", dur, TimingAction.getStartTime(node), StatusExt.fromGenericStatus(status), err);
        return output;
    }

    @Override
    /** Do the final computations to materialize the stage */
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "We can actually get nulls")
    protected void handleChunkDone(@Nonnull MemoryFlowChunk chunk) {
        StageNodeExt stageExt = new StageNodeExt();
        TimingInfo times;
        if (firstExecuted != null) {
            FlowNode last = (chunk.getLastNode() != null) ? chunk.getLastNode() : chunk.getFirstNode(); // Extra safety measure for accidental nesting
            times = StatusAndTiming.computeChunkTiming(run, chunk.getPauseTimeMillis(), firstExecuted, last, chunk.getNodeAfter());
        } else {  // Stage never really ran :)
            times = new TimingInfo(0, 0, run.getStartTimeInMillis());
        }
        ExecDuration dur = (times == null) ? new ExecDuration() : new ExecDuration(times);

        GenericStatus status;
        long startTime = 0;
        if (firstExecuted == null) {
            status = GenericStatus.NOT_EXECUTED;
        } else {
            status = StatusAndTiming.computeChunkStatus(run, chunk.getNodeBefore(), firstExecuted, chunk.getLastNode(), chunk.getNodeAfter());
            startTime = TimingAction.getStartTime(firstExecuted);
        }

        // TODO add and use pipeline graph analysis API to allow us to get most of the metadata for the chunk in 1 pass, efficiently
        //  and only store the FlowNodes -- not the materialized objects.
        stageExt.addBasicNodeData(chunk.getFirstNode(), "", dur, startTime, StatusExt.fromGenericStatus(status), chunk.getLastNode().getError());

        int childNodeLength = Math.min(StageNodeExt.MAX_CHILD_NODES, stageContents.size());
        ArrayList<AtomFlowNodeExt> internals = new ArrayList<AtomFlowNodeExt>(childNodeLength);
        Iterables.addAll(internals, Iterables.limit(stageContents, StageNodeExt.MAX_CHILD_NODES));
        stageExt.setStageFlowNodes(internals);

        stageExt.allChildNodeIds = new ArrayList<String>(Lists.reverse(stageNodeIds));

        this.stages.push(stageExt);
    }

    @Override
    protected void resetChunk(@Nonnull MemoryFlowChunk chunk) {
        super.resetChunk(chunk);
        firstExecuted = null;
        stageNodeIds.clear();
        stageContents.clear();
    }

    @Override
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "We can actually get nulls")
    public void chunkStart(@Nonnull FlowNode startNode, @CheckForNull FlowNode beforeBlock, @Nonnull ForkScanner scanner) {
        if (NotExecutedNodeAction.isExecuted(startNode)) {
            firstExecuted = startNode;
        }
        if (chunk.getLastNode() == null) { // Empty chunk, just a marker with nothing after
            chunk.setLastNode(startNode);
        }
        super.chunkStart(startNode, beforeBlock, scanner);
    }

    /** Called when hitting the end of a block (determined by the chunkEndPredicate) */
    public void chunkEnd(@Nonnull FlowNode endNode, @CheckForNull FlowNode afterBlock, @Nonnull ForkScanner scanner) {
        super.chunkEnd(endNode, afterBlock, scanner);

        // Reset the stage internal state to start here
        stageContents.clear();
        stageNodeIds.clear();
        chunk.setPauseTimeMillis(0);
        firstExecuted = null;

        // if we're using marker-based (and not block-scoped) stages, add the last node as part of its contents
        if (!(endNode instanceof BlockEndNode)) {
            atomNode(null, endNode, afterBlock, scanner);
        }
    }

    public void atomNode(@CheckForNull FlowNode before, @Nonnull FlowNode atomNode, @CheckForNull FlowNode after, @Nonnull ForkScanner scan) {
        if (NotExecutedNodeAction.isExecuted(atomNode)) {
            firstExecuted = atomNode;
        }
        long pause = PauseAction.getPauseDuration(atomNode);
        chunk.setPauseTimeMillis(chunk.getPauseTimeMillis()+pause);

        // TODO this is rather inefficient, we should optimize to use a circular buffer or ArrayList with limited size
        // And then only create the node container objects when we hit the start (doing timing ETC at that point)
        AtomFlowNodeExt ext = makeAtomNode(run, before, atomNode, after);
        if (ext != null) {
            stageContents.push(ext);
        }
        stageNodeIds.add(atomNode.getId());
    }

    @Override
    public void parallelEnd(@Nonnull FlowNode parallelStartNode, @Nonnull FlowNode parallelEndNode, @Nonnull ForkScanner scanner) {
        if (isLastChunk) {
            // Filthy hack, but for incomplete parallels, we use this event to reset the chunk boundaries
            // This works around issues due to branches being visited in order DECLARED, not TIME-ordered
            chunk.setLastNode(parallelEndNode);
        }
        isLastChunk = false;
    }
}
