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

import com.cloudbees.workflow.Util;
import com.cloudbees.workflow.rest.external.RunExt;
import com.cloudbees.workflow.rest.external.StatusExt;
import hudson.model.queue.QueueTaskFuture;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class UndefinedWorkflowTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void testUndefinedRun() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "Noddy Job");

        // Purposely not setting a workflow definition on the job

        QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
        build.waitForStart();

        String jobRunsUrl = job.getUrl() + "wfapi/runs/";
        RunExt[] workflowRuns = Util.getJSON(jobRunsUrl, RunExt[].class, jenkinsRule);

        Assert.assertEquals(1, workflowRuns.length);
        Assert.assertNotNull(workflowRuns[0].getId());
        Assert.assertEquals(StatusExt.NOT_EXECUTED, workflowRuns[0].getStatus());
        assertRunOkay(workflowRuns[0]);

        RunExt runByDescribeLink = Util.getJSON(workflowRuns[0].get_links().self.href, RunExt.class, jenkinsRule);
        assertRunOkay(runByDescribeLink);
    }

    private void assertRunOkay(RunExt workflowRun) {
        Assert.assertEquals(0, workflowRun.getStages().size());
        Assert.assertTrue("Run that should be near instant wasn't", workflowRun.getDurationMillis() < 100);
        Assert.assertEquals(0L, workflowRun.getPauseDurationMillis());
        Assert.assertTrue("Run reports excessive queuing time", workflowRun.getQueueDurationMillis() < 100);
    }
}
