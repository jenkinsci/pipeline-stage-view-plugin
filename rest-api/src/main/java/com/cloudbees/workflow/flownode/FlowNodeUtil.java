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

import com.cloudbees.workflow.rest.external.ExecDuration;
import com.cloudbees.workflow.rest.external.StageNodeExt;
import com.cloudbees.workflow.rest.external.StatusExt;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import hudson.model.Queue;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.NotExecutedNodeAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.support.actions.PauseAction;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class FlowNodeUtil {

    /** When there are at least this elements and we're using a sorted list, switch to binary search */
    static final int BINARY_SEARCH_THRESHOLD = 100;

    static class ExecutionCache {
        List<FlowNode> idSortedExecutionNodeList;
    }

    // Stores ExecutionInfo
    static final Cache<String,ExecutionCache> executionCache = CacheBuilder.newBuilder()
            .weakKeys().weakValues().expireAfterAccess(6, TimeUnit.HOURS).build();

    static final Cache<FlowNode,String> execNodeNameCache = CacheBuilder.newBuilder().weakKeys().build();

    private static final Logger LOGGER = Logger.getLogger(FlowNodeUtil.class.getName());

    private FlowNodeUtil() {
    }

    public static boolean isNotPartOfRunningBuild(FlowExecution execution) {
        Queue.Executable executable = null;
        try {
            executable = execution.getOwner().getExecutable();
        } catch (NullPointerException e) {
            LOGGER.log(Level.FINE, "NullPointerException getting Workflow Queue.Executable. Probably running in a mocked test.");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unexpected error getting Workflow Queue.Executable.", e);
        }

        if (executable != null && executable instanceof Run && !((Run)executable).isBuilding()) {
            return true;
        }
        return false;
    }

    public static long getNodeExecDuration(FlowNode node) {
        long startTime = TimingAction.getStartTime(node);
        if (startTime == 0L) {
            // The node is running and the time has not been marked on it yet.  Return 0 as the duration for now.
            return 0L;
        }

        // This can be replaced by *just* finding the last child node, but it does not seem to help performance
        List<FlowNode> childNodes = getChildNodes(node);
        if (!childNodes.isEmpty()) {
            long endTime = TimingAction.getStartTime(childNodes.get(childNodes.size() - 1));

            return (endTime - startTime);
        } else {
            return 0L;
        }
    }

    public static ExecDuration getStageExecDuration(FlowNode stageStartNode) {
        if (StageNodeExt.isStageNode(stageStartNode)) {
            FlowExecution execution = stageStartNode.getExecution();
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
    public static String getExecNodeName(FlowNode flowNode) {
        String execNodeName = execNodeNameCache.getIfPresent(flowNode);
        if (execNodeName != null) {
            return execNodeName;
        }

        List<FlowNode> parents = flowNode.getParents();

        execNodeName = "master";
        if (parents != null && !parents.isEmpty()) {
            // Don't need to iterate all parents. Working back through one ancestral line
            // should get us to the node containing the WorkspaceAction.
            FlowNode parent = parents.get(0);
            WorkspaceAction executorAction = parent.getAction(WorkspaceAction.class);
            if (executorAction != null) {
                String node = executorAction.getNode();
                if (node.length() > 0) {
                    execNodeName = node;
                }
            } else {
                execNodeName = getExecNodeName(parent);
            }
        }

        execNodeNameCache.put(flowNode, execNodeName);

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

    // Convenience method and optimization, use binary search if more than BINARY_SEARCH_THRESHOLD nodes
    private static int findStageStartNodeIndex(List<FlowNode> allNodesSorted, FlowNode stageStartNode) {
        int stageStartNodeIndex = -1;
        if (allNodesSorted.size() > BINARY_SEARCH_THRESHOLD) {
            stageStartNodeIndex = Collections.binarySearch(allNodesSorted, stageStartNode, sortComparator);
        } else {
            stageStartNodeIndex = allNodesSorted.indexOf(stageStartNodeIndex);
        }
        return stageStartNodeIndex;
    }

    /**
     * Get the last stage executed in the stage.
     * @param stageStartNode The stage start node.
     * @return The last stage executed in the stage.
     */
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
    public static FlowNode getFlowEndNode(FlowExecution execution) {
        // If there's no next stage, then use the last node in the workflow.
        List<FlowNode> endNodes = new ArrayList<FlowNode>(execution.getCurrentHeads());
        sortNodesById(endNodes);

        return endNodes.get(endNodes.size() - 1);
    }

    public static List<FlowNode> getStageNodes(FlowExecution execution) {
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

    // This entirely exists in order to find the last child node for measuring execution time
    // The walker can throw a ConcurrentModificationException if the flow graph changes
    public static List<FlowNode> getChildNodes(final FlowNode parentNode) throws ConcurrentModificationException {
        final List<FlowNode> nodes = new ArrayList<FlowNode>();
        FlowGraphWalker walker = new FlowGraphWalker(parentNode.getExecution());
        for (FlowNode node : walker) {
            if (node.getParents().contains(parentNode)) {  // The walker removes duplicates
                nodes.add(node);
            }
        }
        sortNodesById(nodes);
        return nodes;
    }

    // Throws ConcurrentModificationException if FlowGraph changes under the iterator
    public synchronized static List<FlowNode> getIdSortedExecutionNodeList(FlowExecution execution) throws ConcurrentModificationException {
        if (execution == null || execution.getCurrentHeads().isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        String executionUrl = null;
        boolean isCacheable = false;
        try {
            executionUrl = execution.getUrl();
        } catch (IOException ioe) {
            LOGGER.severe("Can't get execution url for execution, IOException!");
        }
        if (executionUrl != null) {
            ExecutionCache myCache = executionCache.getIfPresent(executionUrl);
            if (myCache != null) {
                return myCache.idSortedExecutionNodeList;
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
            ExecutionCache cacheEntry = new ExecutionCache();
            cacheEntry.idSortedExecutionNodeList = nodes;
            executionCache.put(executionUrl, cacheEntry);
        }
        return nodes;
    }

    static final Comparator<FlowNode> sortComparator = new Comparator<FlowNode>() {
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

    @Restricted(NoExternalUse.class)
    protected static List<FlowNode> sortNodesById(List<FlowNode> nodes) {
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
