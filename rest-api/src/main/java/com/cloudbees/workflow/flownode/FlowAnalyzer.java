package com.cloudbees.workflow.flownode;

import com.cloudbees.workflow.rest.external.AtomFlowNodeExt;
import com.cloudbees.workflow.rest.external.ExecDuration;
import com.cloudbees.workflow.rest.external.FlowNodeExt;
import com.cloudbees.workflow.rest.external.RunExt;
import com.cloudbees.workflow.rest.external.StageNodeExt;
import com.cloudbees.workflow.rest.external.StatusExt;
import com.google.common.collect.Lists;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
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
    public static final String NO_EXEC_NODE = "NOEXECNODE";

    // queue of nodes to visit, outperforms a stack in general use
    protected ArrayDeque<FlowNode> q = new ArrayDeque<>();

    // Maps nodes to their stage *and* tracks visiting of nodes
    protected IdentityHashMap<FlowNode,StageEntry> visited = new IdentityHashMap<FlowNode,StageEntry>();

    protected List<StageEntry> stages = new ArrayList<StageEntry>();

    protected RunExt run = new RunExt();

    protected StageEntry currentStage = new StageEntry();

    protected long lastStartTime = System.currentTimeMillis();

    protected boolean keepChildren = true;

    private int maxChildNodes = 100;

    /** If true, collect child FlowNodes, otherwise do not */
    public boolean isKeepChildren() {
        return keepChildren;
    }

    public void setKeepChildren(boolean keepChildren) {
        this.keepChildren = keepChildren;
    }

    /** Maximum number of child nodes to store under a stage (all will still be analyzed) */
    public int getMaxChildNodes() {
        return maxChildNodes;
    }

    public void setMaxChildNodes(int maxChildNodes) {
        this.maxChildNodes = maxChildNodes;
    }

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
        public List<AtomFlowNodeExt> children = new ArrayList<AtomFlowNodeExt>();
        public long startTime;
        public long endTime;

        public StageNodeExt toStageNodeExt() {
            StageNodeExt node = new StageNodeExt();
            node.addBasicNodeData(stageNode, null, duration, startTime, status, null);  // No exec node, no ErrorAction
            node.setStageFlowNodes(children);
            return node;
        }
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
        run = new RunExt();
        lastStartTime = System.currentTimeMillis();
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

    static Comparator<FlowNodeExt> startTimeComparator = new Comparator<FlowNodeExt>() {
        @Override
        public int compare(FlowNodeExt stage1, FlowNodeExt stage2) {
            long stage1Time = stage1.getStartTimeMillis();
            long stage2Time = stage2.getStartTimeMillis();
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

        StatusExt st = FlowNodeUtil.getStatus(n);
        long pauseMillis = PauseAction.getPauseDuration(n);
        long startTime = TimingAction.getStartTime(n);

        if (entry.nodeCount == 0) { // Final node
            entry.lastChild = n;
            entry.status = st;  // Stage status comes from last node
        }
        entry.nodeCount++;

        // Only create child nodes if we're going to use them
        if (keepChildren && entry.children.size() < maxChildNodes) {
            ExecDuration dur = new ExecDuration();
            dur.setPauseDurationMillis(pauseMillis);
            dur.setTotalDurationMillis(lastStartTime - startTime);

            // FIMXE compute exec node, and don't fetch ErrorAction again
            AtomFlowNodeExt childNode = AtomFlowNodeExt.create(n, NO_EXEC_NODE, dur, startTime, st, n.getError());
            currentStage.children.add(childNode);
        }

        if (st != StatusExt.NOT_EXECUTED) {
            entry.firstExecutedNode = n;
        }

        entry.duration.setPauseDurationMillis(entry.duration.getPauseDurationMillis()+pauseMillis);
        lastStartTime = startTime;
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
            // Cleanup: remove the ultimate stage, if it has a null node, meaning no stage defined
            stages.remove(stages.size()-1);
        }

        for(StageEntry s: stages) {
            if (keepChildren && s.children != null && !s.children.isEmpty()) {
                s.children = Lists.reverse(s.children);
            }
        }

        // They're added in reverse order
        stages = Lists.reverse(stages);

        // CHECKME is this the right place to do this? I think not, we should have done it earlier.
        List<StageNodeExt> materializedNodes = new ArrayList<StageNodeExt>();
        for (StageEntry st : stages) {
            StageNodeExt ext = new StageNodeExt();
            materializedNodes.add(ext);
        }
        run.setStages(materializedNodes);
    }

    /** Run through all points and analyze them */
    public void analyzeAll() {
        while(!q.isEmpty()) {
            FlowNode n = q.pop();
            handleNode(n);
        }
        finishAnalysis();
    }

    /**
     * Analyze a workflow run and create a RunExt from it
     * @param run Run to analyze
     * @param storeChildNodes If true, return StageNodeExt objects with child nodes, otherwise do not
     * @param childNodeLimit
     * @return
     */
    public static RunExt analyzeRunToExt(WorkflowRun run, boolean storeChildNodes, int childNodeLimit) {
        RunExt output = new RunExt();
        RunExt.createMinimal(run);
        FlowAnalyzer analyzer = new FlowAnalyzer(run.getExecution());
        analyzer.run = output;
        analyzer.setKeepChildren(storeChildNodes);
        analyzer.setMaxChildNodes(childNodeLimit);
        analyzer.analyzeAll();

        List<StageNodeExt> realizedStages = new ArrayList<StageNodeExt>(analyzer.stages.size());
        for (StageEntry entry : analyzer.stages) {
            realizedStages.add(entry.toStageNodeExt());
        }
        output.setStages(realizedStages);
        RunExt.computeTimings(output);
        return output;
    }
}
