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
package com.cloudbees.workflow.rest.endpoints.flownode;

import com.cloudbees.workflow.flownode.FlowNodeUtil;
import com.cloudbees.workflow.rest.external.AtomFlowNodeExt;
import com.cloudbees.workflow.rest.external.FlowNodeExt;
import com.cloudbees.workflow.rest.external.StageNodeExt;
import com.cloudbees.workflow.rest.endpoints.FlowNodeAPI;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.jenkinsci.plugins.workflow.graph.AtomNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * {@link FlowNode} "describe" endpoint.
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class Describe {
    private static final Logger LOGGER = Logger.getLogger(Describe.class.getName());

    // Cached because stages are shown *often* and this will greatly boost performance
    // Fixed size because we've limited the number of objects
    static final Cache<String,StageNodeExt> stageExtCache = CacheBuilder.newBuilder().maximumSize(300).expireAfterAccess(1, TimeUnit.HOURS).build();

    @CheckForNull
    public static StageNodeExt getCachedStageNodeExt(FlowNode node) {
        String globalId = null;
        try {
            globalId = node.getExecution().getUrl() + node.getId();
        } catch (IOException ioe) {
            LOGGER.severe("Can't load node execution due to IOExceotion");
            return null;
        }

        StageNodeExt ext = stageExtCache.getIfPresent(globalId);
        if (ext != null) {
            System.out.println("Serving StageExt " + ext.getId() + "from cache");
        }
        return ext;
    }

    public static String getUrl(FlowNode node) {
        return FlowNodeAPI.getUrl(node) + "/describe";
    }

    public static FlowNodeExt get(FlowNode node) {
        if (StageNodeExt.isStageNode(node)) {
            /*StageNodeExt cached = getCachedStageNodeExt(node);
            if(cached != null) {
                // return cached;
            }*/
            StageNodeExt stageNodeExt = StageNodeExt.create(node);
            stageNodeExt.addStageFlowNodes(node);
            /*if (FlowNodeUtil.isNotPartOfRunningBuild(node.getExecution())) {
                try {
                    String globalId = node.getExecution().getUrl()+node.getId();
                    stageExtCache.put(globalId,stageNodeExt);
                } catch (IOException ioe) {
                    LOGGER.severe("Can't load node execution due to IOExceotion");
                }
            }*/
            return stageNodeExt;
        } else if (node instanceof AtomNode) {
            return AtomFlowNodeExt.create(node);
        } else {
            return FlowNodeExt.create(node);
        }
    }
}
