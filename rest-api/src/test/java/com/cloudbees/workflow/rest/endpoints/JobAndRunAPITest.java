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

import com.cloudbees.workflow.flownode.FlowNodeUtil;
import com.cloudbees.workflow.rest.external.BuildArtifactExt;
import com.cloudbees.workflow.rest.external.ChangeSetExt;
import com.cloudbees.workflow.rest.external.JobExt;
import com.cloudbees.workflow.rest.external.RunExt;
import com.cloudbees.workflow.rest.external.StageNodeExt;
import com.cloudbees.workflow.rest.external.StatusExt;
import com.cloudbees.workflow.util.JSONReadWrite;
import com.gargoylesoftware.htmlunit.Page;
import hudson.model.Action;
import hudson.model.queue.QueueTaskFuture;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class JobAndRunAPITest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void test() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "Noddy Job");

        job.setDefinition(new CpsFlowDefinition("" +
                "node {" +
                "   stage ('Build'); " +
                "   echo ('Building'); " +
                "   stage ('Test'); " +
                "   echo ('Testing'); " +
                "   stage ('Deploy'); " +
                (hudson.Functions.isWindows() ? "   bat ('rmdir /s/q targs || echo no such dir\\n mkdir targs && echo hello> targs\\\\hello.txt'); "
                        : "   sh ('rm -rf targs && mkdir targs && echo hello > targs/hello.txt'); "
                ) +
                "   archive(includes: 'targs/hello.txt'); " +
                "   echo ('Deploying'); " +
                "}"));

        QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
        jenkinsRule.assertBuildStatusSuccess(build);

        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();

        assertBasicJobInfoOkay(job, webClient);
        assertBasicRunInfoOkay(job, webClient);
        assertDescribeEndpointOkay(job, webClient);
        assertArtifactsEndpointOkay(job, webClient);
        assertChangesetsEndpointOkay(job, webClient);
        assertTimingHandlesBuggyFlowEndNode(job, webClient);

        // Run another build and then test resultset narrowing using the 'since' query parameter
        build = job.scheduleBuild2(0);
        jenkinsRule.assertBuildStatusSuccess(build);
        assertSinceQueryParamOkay(job, webClient);
    }

    /** Checks handling of one buggy FlowEndNode case */
    private void assertTimingHandlesBuggyFlowEndNode(WorkflowJob job, JenkinsRule.WebClient webClient) throws Exception {
        WorkflowRun run = job.getLastBuild();
        FlowNode lastNode = run.getExecution().getCurrentHeads().get(0);
        List<Action> actions = lastNode.getActions();
        TimingAction timing = lastNode.getAction(TimingAction.class);

        // Remove timing info from last node (root cause of one set of bugs) and flush the cache
        actions.remove(timing);
        FlowNodeUtil.CacheExtension ext = FlowNodeUtil.CacheExtension.all().get(0);
        ext.getExecutionCache().invalidateAll();
        ext.getRunCache().invalidateAll();

        Page runsPage = webClient.goTo(run.getUrl() + "/wfapi/describe", "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();
        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        RunExt runData = jsonReadWrite.fromString(jsonResponse, RunExt.class);
        System.out.println("Duration: "+runData.getDurationMillis());
        assertRunInfoOkay(runData);

        // Restore state
        actions.add(timing);
    }

    private void assertBasicJobInfoOkay(WorkflowJob job, JenkinsRule.WebClient webClient) throws IOException, SAXException {
        String jobRunsUrl = job.getUrl() + "wfapi/describe/";
        Page runsPage = webClient.goTo(jobRunsUrl, "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();

//        System.out.println(jsonResponse);

        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        JobExt jobExt = jsonReadWrite.fromString(jsonResponse, JobExt.class);

        Assert.assertEquals("Noddy Job", jobExt.getName());
        Assert.assertEquals(1, jobExt.getRunCount());
        Assert.assertEquals("/jenkins/job/Noddy%20Job/wfapi/describe", jobExt.get_links().self.href);
        Assert.assertEquals("/jenkins/job/Noddy%20Job/wfapi/runs", jobExt.get_links().getRuns().href);
    }

    private void assertBasicRunInfoOkay(WorkflowJob job, JenkinsRule.WebClient webClient) throws IOException, SAXException {
        String jobRunsUrl = job.getUrl() + "wfapi/runs/";
        Page runsPage = webClient.goTo(jobRunsUrl, "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();

//        System.out.println(jsonResponse);

        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        RunExt[] workflowRuns = jsonReadWrite.fromString(jsonResponse, RunExt[].class);

        Assert.assertEquals(1, workflowRuns.length);
        RunExt runExt = workflowRuns[0];

        assertRunInfoOkay(runExt);
    }

    private void assertDescribeEndpointOkay(WorkflowJob job, JenkinsRule.WebClient webClient) throws IOException, SAXException {
        String descUrl = job.getUrl() + "1/wfapi/describe";
        Page runsPage = webClient.goTo(descUrl, "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();

//        System.out.println(jsonResponse);

        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        RunExt workflowRun = jsonReadWrite.fromString(jsonResponse, RunExt.class);

        assertRunInfoOkay(workflowRun);
    }

    private void assertStageInfoOkay(StageNodeExt stageNodeExt) {
        Assert.assertTrue("Stage has duration <0!", stageNodeExt.getDurationMillis() > 0);
        Assert.assertTrue("Stage has pause duration <0!", stageNodeExt.getPauseDurationMillis() >= 0);
        Assert.assertTrue("Stage has startTimeMillis <= 0!", stageNodeExt.getStartTimeMillis() > 0);
    }

    private void assertRunInfoOkay(RunExt runExt) {
        Assert.assertEquals("#1", runExt.getName());
        Assert.assertEquals(StatusExt.SUCCESS, runExt.getStatus());
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/wfapi/describe", runExt.get_links().self.href);
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/wfapi/artifacts", runExt.get_links().getArtifacts().href);

        Assert.assertEquals(3, runExt.getStages().size());
        Assert.assertEquals("Build", runExt.getStages().get(0).getName());
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/execution/node/5/wfapi/describe", runExt.getStages().get(0).get_links().self.href);
        Assert.assertEquals(StatusExt.SUCCESS, runExt.getStages().get(0).getStatus());
        Assert.assertEquals("Test", runExt.getStages().get(1).getName());
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/execution/node/7/wfapi/describe", runExt.getStages().get(1).get_links().self.href);
        Assert.assertEquals(StatusExt.SUCCESS, runExt.getStages().get(1).getStatus());
        Assert.assertEquals("Deploy", runExt.getStages().get(2).getName());
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/execution/node/9/wfapi/describe", runExt.getStages().get(2).get_links().self.href);
        Assert.assertEquals(StatusExt.SUCCESS, runExt.getStages().get(2).getStatus());

        for (StageNodeExt st : runExt.getStages()) {
            assertStageInfoOkay(st);
        }
    }

    private void assertArtifactsEndpointOkay(WorkflowJob job, JenkinsRule.WebClient webClient) throws IOException, SAXException {
        String artifactsUrl = job.getUrl() + "1/wfapi/artifacts";
        Page runsPage = webClient.goTo(artifactsUrl, "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();

//        System.out.println(jsonResponse);

        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        BuildArtifactExt[] buildArtifactExts = jsonReadWrite.fromString(jsonResponse, BuildArtifactExt[].class);
        Assert.assertEquals(1, buildArtifactExts.length);

        Assert.assertEquals("n1", buildArtifactExts[0].getId());
        Assert.assertEquals("hello.txt", buildArtifactExts[0].getName());
        Assert.assertEquals("targs/hello.txt", buildArtifactExts[0].getPath());
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/artifact/targs/hello.txt", buildArtifactExts[0].getUrl());
        if (hudson.Functions.isWindows()) {
            Assert.assertEquals(7, buildArtifactExts[0].getSize()); // Ends with CRLF
        } else {
            Assert.assertEquals(6, buildArtifactExts[0].getSize()); // Ends with LF only, so one less char
        }
    }

    private void assertChangesetsEndpointOkay(WorkflowJob job, JenkinsRule.WebClient webClient) throws IOException, SAXException {
        String artifactsUrl = job.getUrl() + "1/wfapi/changesets";
        Page runsPage = webClient.goTo(artifactsUrl, "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();

        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        ChangeSetExt[] changeSetExt = jsonReadWrite.fromString(jsonResponse, ChangeSetExt[].class);

        // just testing that the endpoint works.... there's not going to be any changes in it.
        Assert.assertNotNull(changeSetExt);
    }

    private void assertSinceQueryParamOkay(WorkflowJob job, JenkinsRule.WebClient webClient) throws IOException, SAXException {
        RunExt[] workflowRuns;

        // don't use the 'since' param (return them all)
        workflowRuns = getRuns(job, webClient, "wfapi/runs");
        Assert.assertEquals(2, workflowRuns.length);
        Assert.assertEquals("#2", workflowRuns[0].getName());
        Assert.assertEquals("#1", workflowRuns[1].getName());

        // 'since' the first build (return them all)
        workflowRuns = getRuns(job, webClient, "wfapi/runs?since=" + URLEncoder.encode("#1", "UTF-8"));
        Assert.assertEquals(2, workflowRuns.length);
        Assert.assertEquals("#2", workflowRuns[0].getName());
        Assert.assertEquals("#1", workflowRuns[1].getName());

        // 'since' the first build (return them all)
        workflowRuns = getRuns(job, webClient, "wfapi/runs?since=" + URLEncoder.encode("#2", "UTF-8"));
        Assert.assertEquals(1, workflowRuns.length);
        Assert.assertEquals("#2", workflowRuns[0].getName());
    }

    private RunExt[] getRuns(WorkflowJob job, JenkinsRule.WebClient webClient, String url) throws IOException, SAXException {
        Page runsPage = webClient.goTo(job.getUrl() + url, "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();
//        System.out.println(jsonResponse);
        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        return jsonReadWrite.fromString(jsonResponse, RunExt[].class);
    }
}
