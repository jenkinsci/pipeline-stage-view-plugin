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

    // queue of nodes to visit, outperforms a stack in general use
    protected ArrayDeque<FlowNode> q = new ArrayDeque<>();

    // Maps nodes to their stage *and* tracks visiting of nodes
    protected IdentityHashMap<FlowNode,FlowSegment> visited = new IdentityHashMap<FlowNode,FlowSegment>();

    protected List<FlowSegment> stages = new ArrayList<FlowSegment>();

    protected FlowSegment currentStage = new FlowSegment();

    protected long lastStartTime = System.currentTimeMillis();

    protected boolean keepChildren = true;

    /** Maximum number of child nodes to store under a stage, if none are stored, it is -1 */
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
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
            value={"UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD", "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"},
            justification="FlowSegment objects collect run information that may be used many ways")
    public static class FlowSegment {
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

    public List<FlowSegment> getStages() {
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
        currentStage = new FlowSegment();
        currentStage.endTime = System.currentTimeMillis();
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
        FlowSegment stageWithNode = visited.get(n); // Stage associated with this node, if already visited

        // We've just finished walking through a side branch that rejoined a previous-visited path
        // Combine our current stats from the side-branch into the main one
        if (stageWithNode != null) {
            branchEnd(n);
            mergeSideBranchIntoStage(stageWithNode, this.currentStage);
            currentStage = new FlowSegment();
            return;
        }

        // End of current stage, cap it off and start a new that ends where this one began
        if(StageNodeExt.isStageNode(n)) {
            // Cap off the current stage and start a new one
            currentStage.stageNode = n;
            stages.add(currentStage);
            visited.put(n, currentStage);
            long stageStartTime = TimingAction.getStartTime(n);
            currentStage.startTime = stageStartTime; // Node begins with its stage node
            currentStage = new FlowSegment();
            currentStage.endTime = stageStartTime;
        } else {
            updateStageStats(currentStage, n);
            visited.put(n, currentStage);
        }

        List<FlowNode> parents = n.getParents();
        if (parents == null || parents.size() == 0) { // End of the line, bub.  Now we move on to parallel branches.
            stages.add(currentStage);  // has a null stageNode, because we aren't in a defined stage
            currentStage = new FlowSegment();
        } else if (parents.size() == 1) {
            addHead(parents.get(0));
        } else {
            addHeads(parents);
            // Don't need to worry about duration because either of the steps gives a solid estimate
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
    protected void mergeSideBranchIntoStage(FlowSegment mainEntry, FlowSegment sideBranch) {

        if (sideBranch == null || sideBranch.nodeCount == 0) {
            return;  // No branch content
        }

        // Adds the information from a stage side-branch to the main entry
        mainEntry.hasForks = true;
        mainEntry.nodeCount += sideBranch.nodeCount;

        // Add child nodes in, sort, and remove all nodes over our cap
        if (keepChildren) {
            mainEntry.children.addAll(sideBranch.children);
            Collections.sort(mainEntry.children, FlowAnalyzer.startTimeComparator);
            if (maxChildNodes != -1 && mainEntry.children.size() > maxChildNodes) {
                mainEntry.children = mainEntry.children.subList(0, maxChildNodes);
            }
        }

        // We don't need to recompute duration or status, or start/end nodes
        //   because those are taken from the stage's start and end blocks
        //   ... and stages are not permitted inside in parallel steps

        // FIXME this is wrong, we need to stack the parallel branches inside the block I think?
        mainEntry.duration.setPauseDurationMillis(mainEntry.duration.getPauseDurationMillis() + sideBranch.duration.getPauseDurationMillis());;
    }

    // Note that a parallel branch set is beginning, currently unused but can be...
    protected void branchStart(FlowNode node, List<FlowNode> parents) {
        // Used if we are tracking parallel branches
    }

    protected void branchEnd(FlowNode endNode) {
        // Used if we are tracking parallel branches, we've finished a parallel branch now
    }

    /** Modify a stage's stats using the node */
    protected void updateStageStats(FlowSegment entry, FlowNode n ){
        entry.firstChild = n;

        // We can do optimizations to avoid computing these, but it is considerably more complex
        StatusExt st = FlowNodeUtil.getStatus(n);
        long pauseMillis = PauseAction.getPauseDuration(n);
        long startTime = TimingAction.getStartTime(n);

        if (entry.nodeCount == 0) { // Final node
            entry.lastChild = n;
            entry.status = st;  // Stage status comes from last node
        }
        entry.nodeCount++;

        // Only create child nodes if we're going to use them
        if (keepChildren && (maxChildNodes <0 || entry.children.size() < maxChildNodes)) {
            ExecDuration dur = new ExecDuration();
            dur.setPauseDurationMillis(pauseMillis);
            dur.setTotalDurationMillis(lastStartTime - startTime);

            // FIXME use a better algorithm to compute execNodeName, this is O(n) and in fact *wrong*
            // Because it just looks for preceding nodes, while disregarding block scoping
            // We need to track a Stack of block scopes, using the pointers in the BlockEndNodes
            String execNodeName = FlowNodeUtil.getExecNodeName(n);
            //String execNodeName = "master";
            AtomFlowNodeExt childNode = AtomFlowNodeExt.create(n, execNodeName, dur, startTime, st, n.getError());
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
     * Note: we are retaining an implicit empty stage for nodes before first stage statement
     */
    protected void finishAnalysis() {

        if (currentStage.nodeCount != 0) {
            // TODO how do I need to handle this? There shouldn't be any left when we hit the null parent;
        }

        // Reverse children, since they're added in reverse order
        for(FlowSegment s: stages) {
            if (keepChildren && s.children != null && !s.children.isEmpty()) {
                s.children = Lists.reverse(s.children);
            }
        }

        // Fixes for the last node
        if (!stages.isEmpty()) {
            FlowSegment last = stages.get(stages.size()-1);
            if (last.lastChild.isRunning() && !FlowNodeUtil.isPauseNode(last.lastChild)) {
                // If the last node is running and we're not paused, the end time is right now
                last.endTime = System.currentTimeMillis();
            } else {
                // Final stage ends when the last running node starts (it's a FlowEndNode, heh)
                last.endTime = TimingAction.getStartTime(last.lastChild);
            }
        }

        // TIMING COMPUTATION: use first executed node to last time
        for(FlowSegment s : stages) {
            ExecDuration dur = s.duration;
            dur.setPauseDurationMillis(Math.min(dur.getPauseDurationMillis(), dur.getTotalDurationMillis()));
            if (s.status != StatusExt.NOT_EXECUTED) {
                long startTime = TimingAction.getStartTime(s.firstExecutedNode);
                s.startTime = startTime;
                dur.setTotalDurationMillis(s.endTime-startTime);
            } else {
                dur.setTotalDurationMillis(0);
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

    /**
     * Analyze a workflow run and create a RunExt from it
     * @param run Run to analyze
     * @param storeChildNodes If true, return StageNodeExt objects with child nodes, otherwise do not
     * @param childNodeLimit Max number of child nodes to store per StageNodeExt object
     * @return Fully realized RunExt
     */
    public static RunExt analyzeRunToExt(WorkflowRun run, boolean storeChildNodes, int childNodeLimit) {
        RunExt output = RunExt.createMinimal(run);
        FlowExecution exec = run.getExecution();
        if (exec != null) {
            FlowAnalyzer analyzer = new FlowAnalyzer(exec);
            analyzer.setKeepChildren(storeChildNodes);
            analyzer.setMaxChildNodes(childNodeLimit);
            analyzer.analyzeAll();

            List<StageNodeExt> realizedStages = new ArrayList<StageNodeExt>(analyzer.stages.size());
            for (FlowSegment entry : analyzer.stages) {
                if (entry.stageNode != null) {
                    realizedStages.add(entry.toStageNodeExt());
                }
            }
            output.setStages(realizedStages);
        }

        RunExt.computeTimings(output);
        return output;
    }
}
