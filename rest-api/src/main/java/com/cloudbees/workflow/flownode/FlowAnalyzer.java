package com.cloudbees.workflow.flownode;

import com.cloudbees.workflow.rest.external.ExecDuration;
import com.cloudbees.workflow.rest.external.StageNodeExt;
import com.cloudbees.workflow.rest.external.StatusExt;
import com.google.common.collect.Lists;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.support.actions.PauseAction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.ListIterator;

/**
 * Efficiently analyzes a FlowGraph via depth-first search, collecting metrics as it goes.
 *
 * Conveniently enough, this is fully O(n) in number of FlowNodes for all linear graphs.
 * For branches, it does have to do an O(n log n) sorting of child nodes for ordering (sigh)
 *
 * It has hooks to handle and track forks (useful for future rendering), but should perform well in linear cases.
 * It has the starting points to be converted to an extensible version.
 *
 * Threadsafety: no, but not needed.
 *
 * TODO: option to just scan for a single stage
 * @author <a href="mailto:samvanoort@gmail.com">samvanoort@gmail.com</a>
 */
public class FlowAnalyzer  {
    // queue of nodes to visit, outperforms a stack in general use
    protected ArrayDeque<FlowNode> q = new ArrayDeque<>();

    // Maps nodes to their stage *and* tracks visiting of nodes
    protected IdentityHashMap<FlowNode,StageEntry> visited = new IdentityHashMap<FlowNode,StageEntry>();

    protected List<StageEntry> stages = new ArrayList<StageEntry>();

    protected StageEntry currentStage = new StageEntry();


    /** If true, collect child FlowNodes, otherwise do not */
    protected boolean keepChildren = true;

    /** Holds partially-digested information about a stage
     *  This can be used to avoid a lot of backtracking
     */
    public static class StageEntry {
        public boolean hasForks = false;
        public int nodeCount = 0;
        public FlowNode stageNode;
        public FlowNode firstChild;
        public FlowNode firstExecutedNode;
        public FlowNode lastChild;
        public ExecDuration duration = new ExecDuration();
        public StatusExt status = null;
        public List<FlowNode> children = new ArrayList<FlowNode>();
    }

    public List<StageEntry> getStages() {
        return stages;
    }

    /**
     * Resets this to analyze a new execution, so this object can be reused.
     * @param execution Excecution to start a fresh analysis with.
     */
    public void reinitialize(FlowExecution execution) {
        reinitialize(execution.getCurrentHeads());
    }

    public void reinitialize(List<FlowNode> heads) {
        q.clear();
        addHeads(heads);
        addHeads(heads);
        stages.clear();
        visited.clear();
        currentStage = new StageEntry();
    }

    public FlowAnalyzer(FlowExecution exec) {
        reinitialize(exec);
    }

    public FlowAnalyzer() {}
    public FlowAnalyzer(List<FlowNode> heads){
        reinitialize(heads);
    }

    protected void addHead(FlowNode head) {
        q.add(head);
    }

    protected void addHeads(List<FlowNode> heads) {
        ListIterator<FlowNode> itr = heads.listIterator(heads.size());
        while (itr.hasPrevious())
            q.add(itr.previous());
    }

    /** Process a node at a time, adding to stages as appropriate */
    protected void handleNode(FlowNode n) {
        StageEntry stageWithNode = visited.get(n); // Stage associated with this node, if already visited

        // We've just finished walking through a side branch that rejoined a previous-visited path
        // Combine our current stats from the side-branch into the main one
        if (stageWithNode != null) {
            branchEnd(n);
            mergeSideBranchIntoStage(stageWithNode, this.currentStage);
            currentStage = new StageEntry();
            return;
        }

        if(StageNodeExt.isStageNode(n)) {
            // Cap off the current stage and start a new one
            currentStage.stageNode = n;
            stages.add(currentStage);
            visited.put(n, currentStage);
            currentStage = new StageEntry();
        } else {
            updateStageStats(currentStage, n);
            visited.put(n, currentStage);
        }

        List<FlowNode> parents = n.getParents();
        if (parents == null || parents.size() == 0) { // End of the line, bub.  Now we move on to parallel branches.
            stages.add(currentStage);  // has a null stageNode, because we aren't in a defined stage
            currentStage = new StageEntry();
        } else if (parents.size() == 1) {
            addHead(parents.get(0));
            // TODO Parent node needs the time from this, to calc duration
        } else {
            addHeads(parents);
            // TODO Parent node needs the time from this, to calc duration
            branchStart(n, parents);
        }
    }

    static Comparator<FlowNode> startTimeComparator = new Comparator<FlowNode>() {
        @Override
        public int compare(FlowNode stage1, FlowNode stage2) {
            long stage1Time = TimingAction.getStartTime(stage1);
            long stage2Time = TimingAction.getStartTime(stage2);
            if (stage1Time < stage2Time) {
                return -1;
            } else if (stage1Time > stage2Time) {
                return 1;
            }
            return 0;
        }
    };

    /** For parallel branches, we need to combine the stage statistics from each  */
    protected void mergeSideBranchIntoStage(StageEntry mainEntry, StageEntry sideBranch) {

        if (sideBranch == null || sideBranch.nodeCount == 0) {
            return;  // No branch content
        }
        // FIXME handle calculation of durations for side branches
        // FIXME is being called when it shouldn't be, with a 0 size?

        // Adds the information from a stage side-branch to the main entry
        mainEntry.hasForks = true;
        mainEntry.nodeCount += sideBranch.nodeCount;

        if (keepChildren) {
            mainEntry.children.addAll(sideBranch.children);
            Collections.sort(mainEntry.children, FlowAnalyzer.startTimeComparator);
        }

        // TODO status code computation

        // Recompute the pause duration and main duration by combining these
        // Recompute the status code from these
        // Recompute the first and last nodes
        // Recompute if all notexecuted
    }

    // Note that a parallel branch set is beginning, currently unused but can be...
    protected void branchStart(FlowNode node, List<FlowNode> parents) {
        // Used if we are tracking parallel branches
    }

    protected void branchEnd(FlowNode endNode) {
        // Used if we are tracking parallel branches, we've finished a parallel branch now
    }

    /** Modify a stage's stats using the node */
    protected void updateStageStats(StageEntry entry, FlowNode n ){
        entry.firstChild = n;

        // Handle children of stage
        StatusExt st = null;
        if (entry.nodeCount == 0) { // Final node
            entry.lastChild = n;
            st = FlowNodeUtil.getStatus(n);
            entry.status = st;  // Stage status comes from last node
        }

        entry.nodeCount++;
        if (keepChildren) {
            currentStage.children.add(n);
            // Create AtomFlowNodeExts for these?
        }

        // Optimization: if the whole stage succeeded or was not executed, we don't need to check status
        // Or at least not for some basic cases
        StatusExt stageStatus = entry.status;
        if (stageStatus !=  StatusExt.SUCCESS && stageStatus != StatusExt.NOT_EXECUTED) {
            // Now we care about status of the node
            st = FlowNodeUtil.getStatus(n);
            if (st != StatusExt.NOT_EXECUTED) {
                entry.firstExecutedNode = n;
                // FIXME do not compute duration
            }
        }


        long pauseMillis = PauseAction.getPauseDuration(n);
        long pause = PauseAction.getPauseDuration(n);
        entry.duration.setPauseDurationMillis(entry.duration.getPauseDurationMillis()+pause);
    }

    /**
     * Finish analyzing the existing execution, to be called when all nodes in the FlowExecution are exhausted
     */
    protected void finishAnalysis() {

        if (currentStage.nodeCount != 0) {
            // TODO how do I need to handle this? There shouldn't be any left when we hit the null parent;
        }

        StageEntry finalStage = stages.get(stages.size()-1);
        if (finalStage.stageNode == null) {
            // Cleanup: remove the ultimate stage, if it has a null node
            stages.remove(stages.size()-1);
        }

        for(StageEntry s: stages) {
            if (keepChildren && s.children != null && !s.children.isEmpty()) {
                s.children = Lists.reverse(s.children);
            }
        }

        // They're added in reverse order
        stages = Lists.reverse(stages);
    }

    /** Run through all points and analyze them */
    public void analyzeAll() {
        while(!q.isEmpty()) {
            FlowNode n = q.pop();
            handleNode(n);
        }
        finishAnalysis();
    }
}
