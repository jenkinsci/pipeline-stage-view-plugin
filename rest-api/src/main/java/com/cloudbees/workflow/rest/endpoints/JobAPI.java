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
package com.cloudbees.workflow.rest.endpoints;

import com.cloudbees.workflow.rest.AbstractWorkflowJobActionHandler;
import com.cloudbees.workflow.rest.external.JobExt;
import com.cloudbees.workflow.rest.external.RunExt;
import com.cloudbees.workflow.util.ModelUtil;
import com.cloudbees.workflow.util.ServeJson;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.stapler.QueryParameter;

import java.util.List;

/**
 * API Action handler to return WorkflowJob info.
 * <p>
 * Bound to {@code ${{rootURL}/job/<jobname>/wfapi/*}}
 * </p>
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@Extension
public class JobAPI extends AbstractWorkflowJobActionHandler {

    public static String getUrl(WorkflowJob job) {
        return ModelUtil.getFullItemUrl(job.getUrl()) + URL_BASE + "/";
    }

    public static String getDescribeUrl(WorkflowJob job) {
        return getUrl(job) + "describe";
    }

    public static String getRunsUrl(WorkflowJob job) {
        return getUrl(job) + "runs";
    }

    /**
     * Get all Workflow Job runs/builds since the specified run/build name.
     * @param since The run/build name at which to stop returning (inclusive),
     *              or null/empty if all runs/builds are to be returned.
     * @param fullStages Return the stageNodes within each stage
     * @return The runs list.
     */
    @ServeJson
    public List<RunExt> doRuns(@QueryParameter String since, @QueryParameter boolean fullStages) {
        return JobExt.create(getJob().getBuilds(), since, fullStages);
    }

    @ServeJson
    public JobExt doIndex() {
        return doDescribe();
    }

    @ServeJson
    public JobExt doDescribe() {
        return JobExt.create(getJob());
    }
}
