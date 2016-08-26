/*
 * The MIT License
 *
 * Copyright (c) 2014-2016, CloudBees, Inc.
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
package com.cloudbees.workflow.rest.endpoints;

import com.cloudbees.workflow.rest.AbstractFlowNodeActionHandler;
import com.cloudbees.workflow.rest.endpoints.flownode.Describe;
import com.cloudbees.workflow.rest.endpoints.flownode.Log;
import com.cloudbees.workflow.util.ModelUtil;
import com.cloudbees.workflow.util.ServeJson;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.graph.FlowNode;

import java.io.IOException;

/**
 * API Action handler to return {@link FlowNode} information.
 * <p>
 * Bound to {@code ${{rootURL}/job/<jobname>/<runId>/execution/node/<nodeId>/wfapi}}
 * </p>
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@Extension
public class FlowNodeAPI extends AbstractFlowNodeActionHandler {

    public static String getUrl(FlowNode node) {
        return ModelUtil.getFullItemUrl(node) + FlowNodeAPI.URL_BASE;
    }

    @ServeJson
    public Object doIndex() throws IOException {
        return Describe.get(getNode());
    }

    @ServeJson
    public Object doDescribe() throws IOException {
        return Describe.get(getNode());
    }

    @ServeJson
    public Object doLog() {
        return Log.get(getNode());
    }
}
