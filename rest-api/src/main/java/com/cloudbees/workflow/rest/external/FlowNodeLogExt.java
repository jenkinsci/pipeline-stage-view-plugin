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

import com.cloudbees.workflow.util.ModelUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import hudson.console.AnnotatedLargeText;
import org.jenkinsci.plugins.workflow.actions.LogAction;
import org.jenkinsci.plugins.workflow.graph.FlowNode;

import java.io.IOException;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class FlowNodeLogExt {

    private static long MAX_RETURN_CHARS = Integer.getInteger(FlowNodeLogExt.class.getName()+".maxReturnChars", 10 * 1024);

    private static final Logger LOGGER = Logger.getLogger(FlowNodeLogExt.class.getName());

    private String nodeId;
    private StatusExt nodeStatus;
    private long length = 0L;
    private boolean hasMore = false;
    private String text;
    private String consoleUrl; // Not a rest endpoint so not including in _links

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public StatusExt getNodeStatus() {
        return nodeStatus;
    }

    public void setNodeStatus(StatusExt nodeStatus) {
        this.nodeStatus = nodeStatus;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getConsoleUrl() {
        return consoleUrl;
    }

    public void setConsoleUrl(String consoleUrl) {
        this.consoleUrl = consoleUrl;
    }

    public static FlowNodeLogExt create(FlowNode node) {
        FlowNodeLogExt logExt = new FlowNodeLogExt();

        logExt.setNodeId(node.getId());
        logExt.setNodeStatus(StatusExt.valueOf(node.getError()));

        LogAction logAction = node.getAction(LogAction.class);
        if (logAction != null) {
            logExt.setConsoleUrl(ModelUtil.getFullItemUrl(node) + logAction.getUrlName());

            AnnotatedLargeText<? extends FlowNode> logText = logAction.getLogText();
            if (logText != null) {
                long logLen = logText.length();

                logExt.setLength(Math.min(MAX_RETURN_CHARS, logLen));
                logExt.setHasMore((logLen > MAX_RETURN_CHARS));

                if (logLen > 0) {
                    StringWriter writer = new StringWriter();
                    try {
                        logText.writeHtmlTo(logLen - logExt.getLength(), writer);
                        logExt.setText(writer.toString());
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Error serializing log for", e);
                    }
                }
            }
        }

        return logExt;
    }

}
