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
package com.cloudbees.workflow.rest.external;

import com.cloudbees.workflow.flownode.FlowNodeUtil;
import com.cloudbees.workflow.rest.endpoints.flownode.Describe;
import com.cloudbees.workflow.rest.hal.Link;
import com.cloudbees.workflow.rest.hal.Links;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.NotExecutedNodeAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.support.actions.PauseAction;
import org.kohsuke.stapler.Stapler;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class FlowNodeExt {
    private FlowNodeLinks _links;
    private String id;
    private String name;
    private String execNode;
    private StatusExt status;
    private ErrorExt error;
    private long startTimeMillis;
    private long durationMillis;
    private long pauseDurationMillis;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public FlowNodeLinks get_links() {
        return _links;
    }

    public void set_links(FlowNodeLinks _links) {
        this._links = _links;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getExecNode() {
        return execNode;
    }

    public void setExecNode(String execNode) {
        this.execNode = execNode;
    }

    public StatusExt getStatus() {
        return status;
    }

    public void setStatus(StatusExt status) {
        this.status = status;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ErrorExt getError() {
        return error;
    }

    public void setError(ErrorExt error) {
        this.error = error;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public long getPauseDurationMillis() {
        return pauseDurationMillis;
    }

    public void setPauseDurationMillis(long pauseDurationMillis) {
        this.pauseDurationMillis = pauseDurationMillis;
    }

    public static final class FlowNodeLinks extends Links {
        private Link log;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Link getLog() {
            return log;
        }

        public void setLog(Link log) {
            this.log = log;
        }
    }

    public static FlowNodeExt create(FlowNode node) {
        FlowNodeExt flowNodeExt = new FlowNodeExt();
        flowNodeExt.addBasicNodeData(node);
        return flowNodeExt;
    }

    public static FlowNodeExt create(FlowNode node, String execNodeName, ExecDuration duration, long startTimeMillis,
                                     StatusExt status, ErrorAction error) {
        FlowNodeExt flowNodeExt = new FlowNodeExt();
        flowNodeExt.addBasicNodeData(node, execNodeName, duration, startTimeMillis, status, error);
        return flowNodeExt;
    }

    protected void calculateTimings(FlowNode node) {
        if (getStatus() != StatusExt.NOT_EXECUTED) {
            setStartTimeMillis(TimingAction.getStartTime(node));
            setDurationMillis(FlowNodeUtil.getNodeExecDuration(node));
            setPauseDurationMillis(PauseAction.getPauseDuration(node));
            setPauseDurationMillis(Math.min(getPauseDurationMillis(), getDurationMillis()));
        }
    }

    // Allows for passing in a node with all the key information filled in
    public void addBasicNodeData(FlowNode node, String execNodeName, ExecDuration duration, long startTimeMillis, StatusExt status, ErrorAction error) {

        setId(node.getId());
        setName(node.getDisplayName());
        setExecNode(execNodeName);
        set_links(new FlowNodeLinks());
        get_links().initSelf(Describe.getUrl(node));
        setStatus(status);
        if (status != StatusExt.NOT_EXECUTED) {
            setError(ErrorExt.create(error));
        }

        this.setStartTimeMillis(startTimeMillis);
        if (duration != null) {
            this.setPauseDurationMillis(duration.getPauseDurationMillis());
            this.setDurationMillis(duration.getTotalDurationMillis());
        }
    }

    protected void addBasicNodeData(FlowNode node) {
        String execNodeName = FlowNodeUtil.getExecNodeName(node);
        boolean isExecuted = NotExecutedNodeAction.isExecuted(node);
        StatusExt status = null;
        ErrorAction errorAction = null;
        if (isExecuted) {
            errorAction = node.getError();
            status = StatusExt.valueOf(errorAction);
        } else {
            status = StatusExt.NOT_EXECUTED;
        }

        // Placeholders are used for timing data until calculated explicitly
        addBasicNodeData(node, execNodeName, null, 0L, status, errorAction);
        calculateTimings(node);
    }

    @Override public String toString() {
        return "FlowNodeExt[id=" + getId() + ",name=" + getName() + "]";
    }
}
