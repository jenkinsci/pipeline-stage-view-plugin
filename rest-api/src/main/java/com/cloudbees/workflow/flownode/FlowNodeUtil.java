
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

import com.cloudbees.workflow.rest.external.CacheStatsExt;
import com.cloudbees.workflow.rest.external.ExecDuration;
import com.cloudbees.workflow.rest.external.RunExt;
import com.cloudbees.workflow.rest.external.StageNodeExt;
import com.cloudbees.workflow.rest.external.StatusExt;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.NotExecutedNodeAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.support.actions.PauseAction;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class FlowNodeUtil {


    private static final Logger LOGGER = Logger.getLogger(FlowNodeUtil.class.getName());

    private FlowNodeUtil() {
    }

    // Used in unit testing where Jenkins is not running yet
    private static final CacheExtension FALLBACK_CACHES = new CacheExtension();

    @Extension
    @Restricted(NoExternalUse.class)
    public static final class CacheExtension {

        protected final Cache<String,List<FlowNode>> executionCache = CacheBuilder.newBuilder().weakValues().maximumSize(100).build();

        // Larger cache of run data, for completed runs, keyed by flowexecution url, useful for serving info
        // Actually can be used to serve Stage data too
        // Because the RunExt caps the total elements returned, and this is fully realized, this is the fastest way
        protected final Cache<String, RunExt> runData = CacheBuilder.newBuilder().maximumSize(1000).build();

        protected final Cache<FlowNode,String> execNodeNameCache = CacheBuilder.newBuilder().weakKeys().expireAfterAccess(1, TimeUnit.HOURS).build();

        public Cache<String,List<FlowNode>> getExecutionCache() {
            return this.executionCache;
        }
        public Cache<String, RunExt> getRunCache() {
            return this.runData;
        }

        public Cache<FlowNode,String> getExecNodeNameCache() {
            return  this.execNodeNameCache;
        }

        public static CacheExtension get() {
            Jenkins myJenkins = Jenkins.getInstance();
            if ( myJenkins == null) {
                return FALLBACK_CACHES;
            } else {
                return myJenkins.getExtensionList(CacheExtension.class).get(0);
            }
        }

        /**
         * Get a cached run if present, return null if not
         * @param ex FlowExecution, null in
         * @return RunExt if present in cache, otherwise null if absent or a null execution given
         */
        @CheckForNull
        public RunExt getCachedRun(@CheckForNull FlowExecution ex) {
            try {
                if (ex != null) {
                    RunExt cachedRun = getRunCache().getIfPresent(ex.getUrl());
                    if (cachedRun != null) {
                        return cachedRun;
                    }
                }
                return null;
            } catch (IOException ioe) {
                LOGGER.log(Level.SEVERE, null, ioe);
                return null;
            }
        }

        /**
         * Cache a processed pipeline run
         * @param exec Execution for that run
         * @param run Run that was processed
         */
        public void cacheRun(@Nonnull FlowExecution exec, @Nonnull RunExt run) {
            if (exec != null && exec.isComplete()) {
                try {
                    getRunCache().put(exec.getUrl(), run);
                } catch (IOException ioe) {
                    LOGGER.log(Level.SEVERE, null, ioe);
                }
            }
        }
    }

    // Used to return information about caches used for FlowNodeUtil
    public static class CacheResultsExt {
        protected CacheStatsExt executionCacheStats;
        protected CacheStatsExt runDataCacheStats;
        protected CacheStatsExt execNodeNameCacheStats;

        public CacheStatsExt getExecutionCacheStats() {
            return executionCacheStats;
        }

        public void setExecutionCacheStats(CacheStatsExt executionCacheStats) {
            this.executionCacheStats = executionCacheStats;
        }

        public CacheStatsExt getRunDataCacheStats() {
            return runDataCacheStats;
        }

        public void setRunDataCacheStats(CacheStatsExt runDataCacheStats) {
            this.runDataCacheStats = runDataCacheStats;
        }

        public CacheStatsExt getExecNodeNameCacheStats() {
            return execNodeNameCacheStats;
        }

        public void setExecNodeNameCacheStats(CacheStatsExt execNodeNameCacheStats) {
            this.execNodeNameCacheStats = execNodeNameCacheStats;
        }

        public CacheResultsExt(Cache runDataCache, Cache executionCache, Cache execNodeNameCache) {
            this.setRunDataCacheStats(new CacheStatsExt(runDataCache));
            this.setExecutionCacheStats(new CacheStatsExt(executionCache));
            this.setExecNodeNameCacheStats(new CacheStatsExt(execNodeNameCache));
        }
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="URF_UNREAD_FIELD")  // Findbugs being silly
    public static CacheResultsExt getCacheResults() {
        CacheExtension caches = CacheExtension.get();
        CacheResultsExt output = new CacheResultsExt(caches.getRunCache(), caches.getExecutionCache(), caches.getExecNodeNameCache());
        return output;
    }

    /**
     * Get a cached run if present, return null if not
     * @param ex FlowExecution, null in
     * @return RunExt if present in cache, otherwise null if absent or a null execution given
     */
    @CheckForNull
    public static RunExt getCachedRun(@CheckForNull FlowExecution ex) {
        return CacheExtension.get().getCachedRun(ex);
    }

    public static void cacheRun(@Nonnull FlowExecution exec, @Nonnull RunExt run) {
        CacheExtension.get().cacheRun(exec, run);
    }

    public static boolean isNotPartOfRunningBuild(FlowExecution execution) {
        return (execution != null && execution.isComplete());
    }

    public static long getNodeExecDuration(FlowNode node) {
        long startTime = TimingAction.getStartTime(node);
        if (startTime == 0L) {
            // The node is running and the time has not been marked on it yet.  Return 0 as the duration for now.
            return 0L;
        }

        // All we need to calculate exec duration is the last child nodes
        FlowNode lastChild = getLastChildNode(node);
        if (lastChild != null) {
            long endTime = TimingAction.getStartTime(lastChild);
            return (endTime - startTime);
        } else {
            return 0L;
        }
    }

    public static FlowNode getLastChildNode(FlowNode node) {
        List<FlowNode> allNodes = getIdSortedExecutionNodeList(node.getExecution());
        for(int i=allNodes.size()-1; i>=0; i--) {
            FlowNode f = allNodes.get(i);
            if (f.getParents().contains(node)) {
                return f;
            }
        }
        return null;
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
        justification = "Precondition of block ensures that null cannot be returned (FlowExecution checked)")
    public static ExecDuration getStageExecDuration(FlowNode stageStartNode) {
        FlowExecution execution = stageStartNode.getExecution();
        if (execution != null && StageNodeExt.isStageNode(stageStartNode)) {
            List<FlowNode> allNodesSorted = getIdSortedExecutionNodeList(execution);
            int stageStartNodeIndex = findStageStartNodeIndex(allNodesSorted, stageStartNode);
            FlowNode firstExecutedNode;

            // Locate the first node in the stage that was actually executed.  This can
            // vary e.g. when there's was a checkpoint restart, the first executed node
            // in the stage could be in middle of the stage Pipeline definition.
            firstExecutedNode = getStageExecStartNode(allNodesSorted, stageStartNodeIndex);

            if (firstExecutedNode != null) {
                long startTime = TimingAction.getStartTime(firstExecutedNode);
                long endTime;

                // The stage end time is either the start time of the next stage, or the start time
                // of the last node started on the Pipeline (if there is no next stage).
                FlowNode nextStageNode = getNextStageNode(allNodesSorted, stageStartNodeIndex);
                if (nextStageNode != null) {
                    endTime = TimingAction.getStartTime(nextStageNode);
                } else {
                    FlowNode flowEndNode = getFlowEndNode(execution);
                    endTime = TimingAction.getStartTime(flowEndNode);

                    // If the node is running then we might want to use the "now" time as the end time.
                    // Otherwise we are using the start time of the node that is running, which is not
                    // changing i.e. will look as though the node is not running.
                    if (flowEndNode.isRunning() && !execution.isComplete()) {
                        // But only do this if the node is not paused e.g. for input.
                        if (!isPauseNode(flowEndNode)) {
                            long currentTime = System.currentTimeMillis();
                            if (currentTime > endTime) {
                                endTime = currentTime;
                            }
                        }
                    }
                }

                ExecDuration execDuration = new ExecDuration();
                if (startTime != 0) {
                    execDuration.setTotalDurationMillis(endTime - startTime);

                    // Calculate the stage pause duration.
                    int nextStageIndex;
                    if (nextStageNode != null) {
                        nextStageIndex = findStageStartNodeIndex(allNodesSorted, nextStageNode);
                    } else {
                        nextStageIndex = allNodesSorted.size(); // +1 from end of list
                    }
                    for (int i = stageStartNodeIndex; i < nextStageIndex; i++) {
                        FlowNode node = allNodesSorted.get(i);
                        execDuration.setPauseDurationMillis(execDuration.getPauseDurationMillis() + PauseAction.getPauseDuration(node));
                    }
                }
                return execDuration;
            }
        }

        return new ExecDuration();
    }

    /**
     * Is the supplied node causing the workflow to pause at that point.
     * @param flowNode The node.
     * @return True if the node is causing the workflow to pause, otherwise false.
     */
    public static boolean isPauseNode(FlowNode flowNode) {
        return PauseAction.isPaused(flowNode);
    }

    /**
     * Get the first node in the stage that executed.
     * <p>
     * An executed node is a node that does not have a {@link NotExecutedNodeAction} action.
     * @param stageStartNode The stage start node (the stage node).
     * @return The first node in the stage that executed.  This can be the stage node itself.
     */
    public static FlowNode getStageExecStartNode(FlowNode stageStartNode) {
        FlowExecution execution = stageStartNode.getExecution();
        List<FlowNode> allNodesSorted = getIdSortedExecutionNodeList(execution);
        int stageStartNodeIndex = findStageStartNodeIndex(allNodesSorted, stageStartNode);

        return getStageExecStartNode(allNodesSorted, stageStartNodeIndex);
    }

    private static FlowNode getStageExecStartNode(List<FlowNode> allNodesSorted, int stageStartNodeIndex) {
        for (int i = stageStartNodeIndex; i < allNodesSorted.size(); i++) {
            FlowNode node = allNodesSorted.get(i);

            if (i > stageStartNodeIndex && StageNodeExt.isStageNode(node)) {
                // we've reached the end of the stage, so seems like nothing was executed in it
                return null;
            }
            if (NotExecutedNodeAction.isExecuted(node)) {
                long nodeStartTime = TimingAction.getStartTime(node);
                if (nodeStartTime > 0) {
                    return node;
                }
            }
        }
        return null;
    }

    /**
     * Get the name of the node on which the supplied FlowNode executed.
     *
     * @param flowNode The node.
     * @return The name of the node on which the supplied FlowNode executed.
     */
    public static String getExecNodeName(@Nonnull FlowNode flowNode) {

        Cache<FlowNode, String> execNodeNameCache = CacheExtension.get().getExecNodeNameCache();
        String execNodeName = execNodeNameCache.getIfPresent(flowNode);
        if (execNodeName != null) {
            return execNodeName;
        }

        // It is sufficient to walk through just the ancestry
        ArrayDeque<FlowNode> ancestry = new ArrayDeque<FlowNode>(); // For these nodes, add cache entries

        while (flowNode != null) {
            // Always hit the cache first
            execNodeName = execNodeNameCache.getIfPresent(flowNode);
            if (execNodeName != null) {
                break;
            }

            // Then we check for the node itself
            WorkspaceAction executorAction = flowNode.getAction(WorkspaceAction.class);
            if (executorAction != null) {
                String node = executorAction.getNode();
                if (node.length() > 0) {
                    execNodeName = node;
                }
                break;
            }

            // Next look at ancestors
            ancestry.push(flowNode);
            List<FlowNode> parents = flowNode.getParents();
            if (parents != null && !parents.isEmpty()) {
                flowNode = parents.get(0);
            } else {
                break;
            }
        }
        // Ran on master if no executor workspace
        if (execNodeName == null) {
            execNodeName = "master";
        }
        for (FlowNode f : ancestry) {
            execNodeNameCache.put(f, execNodeName);
        }
        return execNodeName;
    }

    /**
     * Get the first node in next stage, given the first node in the previous stage (the stage start node).
     * @param stageStartNode The stage start node.
     * @return The first node in the next stage, or null if there is no next stage.
     */
    public static FlowNode getNextStageNode(FlowNode stageStartNode) {
        FlowExecution execution = stageStartNode.getExecution();
        List<FlowNode> allNodesSorted = getIdSortedExecutionNodeList(execution);

        int stageStartNodeIndex = findStageStartNodeIndex(allNodesSorted, stageStartNode);

        return getNextStageNode(allNodesSorted, stageStartNodeIndex);
    }

    private static FlowNode getNextStageNode(List<FlowNode> allNodesSorted, int stageStartNodeIndex) {
        for (int i = (stageStartNodeIndex + 1); i < allNodesSorted.size(); i++) {
            FlowNode node = allNodesSorted.get(i);
            if (StageNodeExt.isStageNode(node)) {
                return node;
            }
        }

        return null;
    }

    // Pulled out because it is likely to get re-implemented
    private static int findStageStartNodeIndex(List<FlowNode> allNodesSorted, FlowNode stageStartNode) {
        return  allNodesSorted.indexOf(stageStartNode);
    }

    /**
     * Get the last stage executed in the stage.
     * @param stageStartNode The stage start node.
     * @return The last stage executed in the stage.
     */
    @CheckForNull
    public static FlowNode getStageEndNode(FlowNode stageStartNode) {
        FlowExecution execution = stageStartNode.getExecution();
        List<FlowNode> allNodesSorted = getIdSortedExecutionNodeList(execution);

        int stageStartNodeIndex = findStageStartNodeIndex(allNodesSorted, stageStartNode);

        for (int i = (stageStartNodeIndex + 1); i < allNodesSorted.size(); i++) {
            FlowNode node = allNodesSorted.get(i);
            if (StageNodeExt.isStageNode(node)) {
                return allNodesSorted.get(i - 1);
            }
        }

        return allNodesSorted.get(allNodesSorted.size() - 1);
    }


    /**
     * Get the last node to start in a flow.
     * @param execution The flow execution.
     * @return The last node to start in the flow.
     */
    @CheckForNull
    public static FlowNode getFlowEndNode(@CheckForNull  FlowExecution execution) {
        if (execution == null) {
            return null;
        }

        // If there's no next stage, then use the last node in the workflow.
        List<FlowNode> endNodes = new ArrayList<FlowNode>(execution.getCurrentHeads());
        sortNodesById(endNodes);

        return endNodes.get(endNodes.size() - 1);
    }

    public static List<FlowNode> getStageNodes(@CheckForNull FlowExecution execution) {

        if (execution == null) {
            return Collections.EMPTY_LIST;
        }

        List<FlowNode> nodes = new ArrayList<FlowNode>();
        List<FlowNode> allNodesSorted = getIdSortedExecutionNodeList(execution);

        for (int i = 0; i < allNodesSorted.size(); i++) {
            FlowNode sortedNode = allNodesSorted.get(i);
            if (StageNodeExt.isStageNode(sortedNode)) {
                nodes.add(sortedNode);
            }
        }

        return nodes;
    }

    // Enables us to get the status of a node without creating a bunch of objects
    public static StatusExt getStatus(FlowNode node) {
        boolean isExecuted = NotExecutedNodeAction.isExecuted(node);

        if (isExecuted) {
            ErrorAction errorAction = node.getError();
            return StatusExt.valueOf(errorAction);
        } else {
            return StatusExt.NOT_EXECUTED;
        }
    }

    public static List<FlowNode> getStageNodes(FlowNode node) {
        List<FlowNode> nodes = new ArrayList<FlowNode>();
        List<FlowNode> allNodesSorted = getIdSortedExecutionNodeList(node.getExecution());

        int stageStartNodeIndex = findStageStartNodeIndex(allNodesSorted, node);

        if (StageNodeExt.isStageNode(node)) {
            // Starting at the node after the supplied node, add all sorted nodes up to the
            // next stage (or the end of the workflow)...
            stageStartNodeIndex++;
            for (int i = stageStartNodeIndex; i < allNodesSorted.size(); i++) {
                FlowNode sortedNode = allNodesSorted.get(i);
                if (StageNodeExt.isStageNode(sortedNode)) {
                    break;
                }
                nodes.add(sortedNode);
            }
        }

        return nodes;
    }

    // Throws ConcurrentModificationException if FlowGraph changes under the iterator
    public static List<FlowNode> getIdSortedExecutionNodeList(FlowExecution execution) throws ConcurrentModificationException {
        if (execution == null || execution.getCurrentHeads().isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        String executionUrl = null;
        boolean isCacheable = false;
        try {
            executionUrl = execution.getUrl();
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, null, ioe);
        }
        Cache<String,List<FlowNode>> cache = CacheExtension.get().getExecutionCache();
        if (executionUrl != null) {
            List<FlowNode> sortedList = cache.getIfPresent(executionUrl);
            if (sortedList != null) {
                return sortedList;
            }
        }

        if (isNotPartOfRunningBuild(execution)) {
            isCacheable = true;
        }

        // Not in cache or can't cache
        FlowGraphWalker walker = new FlowGraphWalker(execution);
        ArrayList<FlowNode> nodes = new ArrayList<FlowNode>();
        for (FlowNode node : walker) {
            nodes.add(node);
        }
        sortNodesById(nodes);

        if (isCacheable && executionUrl != null) {
            cache.put(executionUrl, nodes);
        }
        return nodes;
    }


    /** This is used to cover an obscure case where a WorkflowJob is renamed BUT
     *  a previous WorkflowJob existed with cached execution data.
     *  Otherwise the previous job's cached data would be returned.
     **/
    @Extension
    public static class RenameHandler extends ItemListener {
        @Override
        public void onLocationChanged(Item item, String oldFullName, String newFullName) {
            if (item instanceof WorkflowJob) {
                // It's a cache, we can rebuild it... we have the technology.
                CacheExtension ext = CacheExtension.get();
                ext.getExecutionCache().invalidateAll();
                ext.getRunCache().invalidateAll();
            }
        }
    }

    public static final Comparator<FlowNode> sortComparator = new Comparator<FlowNode>() {
        @Override
        public int compare(FlowNode node1, FlowNode node2) {
            int node1Iota = parseIota(node1);
            int node2Iota = parseIota(node2);

            if (node1Iota < node2Iota) {
                return -1;
            } else if (node1Iota > node2Iota) {
                return 1;
            }
            return 0;
        }

        private int parseIota(FlowNode node) {
            try {
                return Integer.parseInt(node.getId());
            } catch (NumberFormatException e) {
                LOGGER.severe("Failed to parse FlowNode ID '" + node.getId() + "' on step '" + node.getDisplayName() + "'.  Expecting iota to be an integer value.");
                return 0;
            }
        }
    };

    public static List<FlowNode> sortNodesById(List<FlowNode> nodes) {
        Collections.sort(nodes, sortComparator);
        return nodes;
    }

    /**
     * Simple debug utility for dumping a node list to sysout.
     * @param nodeList The list to dump.
     */
    public static void dumpNodes(List<FlowNode> nodeList) {
        System.out.println("------------------------------------------------------------------------------------------");
        for (FlowNode node : nodeList) {
            System.out.println("[" + node.getId() + "][" + TimingAction.getStartTime(node) + "] " + node.getDisplayName());
        }
        System.out.println("------------------------------------------------------------------------------------------");
    }
}
