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
import com.cloudbees.workflow.rest.external.AtomFlowNodeExt;
import com.cloudbees.workflow.rest.external.FlowNodeLogExt;
import com.cloudbees.workflow.rest.external.RunExt;
import com.cloudbees.workflow.rest.external.StageNodeExt;
import com.cloudbees.workflow.rest.external.StatusExt;
import com.cloudbees.workflow.util.JSONReadWrite;
import com.gargoylesoftware.htmlunit.Page;

import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import org.jenkinsci.plugins.workflow.steps.ErrorStep;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class FlowNodeAPITest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void test_success_flow() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "Noddy Job");
        String jobRunsUrl = job.getUrl() + "wfapi/runs/";

        job.setDefinition(new CpsFlowDefinition("" +
                "node {" +
                "   stage ('Build'); " +
                "   echo ('Building'); " +
                "   stage ('Test'); " +
                "   echo ('Testing'); " +
                "   stage ('Deploy'); " +
                "   echo ('Deploying'); " +
                "}"));

        QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
        jenkinsRule.assertBuildStatusSuccess(build);

        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();

        Page runsPage = webClient.goTo(jobRunsUrl, "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();

//        System.out.println(jsonResponse);

        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        RunExt[] workflowRuns = jsonReadWrite.fromString(jsonResponse, RunExt[].class);

        Assert.assertEquals(1, workflowRuns.length);
        Assert.assertEquals("#1", workflowRuns[0].getName());
        Assert.assertEquals(StatusExt.SUCCESS, workflowRuns[0].getStatus());

        // Test the endpoints
        assert_describe_ok(webClient, jsonReadWrite, workflowRuns);
        assert_log_ok(webClient, jsonReadWrite, workflowRuns);
    }

    private void assert_describe_ok(JenkinsRule.WebClient webClient, JSONReadWrite jsonReadWrite, RunExt[] workflowRuns) throws IOException, SAXException {
        String jsonResponse;
        List<StageNodeExt> stages = workflowRuns[0].getStages();
        Assert.assertEquals(3, stages.size());
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/execution/node/5/wfapi/describe", stages.get(0).get_links().self.href);
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/execution/node/7/wfapi/describe", stages.get(1).get_links().self.href);
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/execution/node/9/wfapi/describe", stages.get(2).get_links().self.href);

        Page stageDescription = webClient.goTo("job/Noddy%20Job/1/execution/node/5/wfapi/describe", "application/json");
        jsonResponse = stageDescription.getWebResponse().getContentAsString();

//        System.out.println(jsonResponse);
        StageNodeExt stageDesc = jsonReadWrite.fromString(jsonResponse, StageNodeExt.class);

        Assert.assertEquals("5", stageDesc.getId());
        Assert.assertEquals("Build", stageDesc.getName());
        Assert.assertEquals(StatusExt.SUCCESS, stageDesc.getStatus());
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/execution/node/5/wfapi/describe", stageDesc.get_links().self.href);
        Assert.assertEquals(1, stageDesc.getStageFlowNodes().size());
        Assert.assertEquals("6", stageDesc.getStageFlowNodes().get(0).getId());
        Assert.assertEquals("Print Message", stageDesc.getStageFlowNodes().get(0).getName());
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/execution/node/6/wfapi/describe", stageDesc.getStageFlowNodes().get(0).get_links().self.href);
        Assert.assertEquals("[5]", stageDesc.getStageFlowNodes().get(0).getParentNodes().toString());
        Assert.assertEquals(StatusExt.SUCCESS, stageDesc.getStageFlowNodes().get(0).getStatus());

        for (StageNodeExt st : stages) {
            validateChildNodeDescribeAPIs(st, webClient);
        }
    }

    private void assert_log_ok(JenkinsRule.WebClient webClient, JSONReadWrite jsonReadWrite, RunExt[] workflowRuns) throws IOException, SAXException {
        String jsonResponse;
        List<StageNodeExt> stages = workflowRuns[0].getStages();

        Page stageDescription = webClient.goTo(Util.removeRootUrl(stages.get(0).get_links().self.href), "application/json");
        jsonResponse = stageDescription.getWebResponse().getContentAsString();
        StageNodeExt stageDesc = jsonReadWrite.fromString(jsonResponse, StageNodeExt.class);

        String logUrl = stageDesc.getStageFlowNodes().get(0).get_links().getLog().href;
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/execution/node/6/wfapi/log", logUrl);

        Page nodeLog = webClient.goTo(Util.removeRootUrl(logUrl), "application/json");
        jsonResponse = nodeLog.getWebResponse().getContentAsString();
        FlowNodeLogExt logExt = jsonReadWrite.fromString(jsonResponse, FlowNodeLogExt.class);

        Assert.assertEquals("6", logExt.getNodeId());
        Assert.assertEquals(StatusExt.SUCCESS, logExt.getNodeStatus());
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/execution/node/6/log", logExt.getConsoleUrl());
        Assert.assertNotNull(logExt.getText());
        Assert.assertThat(logExt.getText(), containsString("Building"));
    }

    @Test
    public void test_failed_flow() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "Noddy Job");
        String jobRunsUrl = job.getUrl() + "wfapi/runs/";

        job.setDefinition(new CpsFlowDefinition("" +
                "node {" +
                "   stage ('Build'); " +
                "   error ('my specific failure message'); " +
                "   stage ('Test'); " +
                "   error ('echo Testing'); " +
                "   stage ('Deploy'); " +
                "   error ('echo Deploying'); " +
                "}"));

        QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
        jenkinsRule.assertBuildStatus(Result.FAILURE, build.get());

        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();

        Page runsPage = webClient.goTo(jobRunsUrl, "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();

//        System.out.println(jsonResponse);

        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        RunExt[] workflowRuns = jsonReadWrite.fromString(jsonResponse, RunExt[].class);

        Assert.assertEquals(1, workflowRuns.length);
        Assert.assertEquals("#1", workflowRuns[0].getName());
        Assert.assertEquals(StatusExt.FAILED, workflowRuns[0].getStatus());

        List<StageNodeExt> stages = workflowRuns[0].getStages();
        Assert.assertEquals(1, stages.size());
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/execution/node/5/wfapi/describe", stages.get(0).get_links().self.href);

        Page stageDescription = webClient.goTo("job/Noddy%20Job/1/execution/node/5/wfapi/describe", "application/json");
        jsonResponse = stageDescription.getWebResponse().getContentAsString();

//        System.out.println(jsonResponse);
        StageNodeExt stageDesc = jsonReadWrite.fromString(jsonResponse, StageNodeExt.class);

        Assert.assertEquals("5", stageDesc.getId());
        Assert.assertEquals("Build", stageDesc.getName());
        Assert.assertEquals(StatusExt.FAILED, stageDesc.getStatus());
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/execution/node/5/wfapi/describe", stageDesc.get_links().self.href);
        Assert.assertEquals(1, stageDesc.getStageFlowNodes().size());
        Assert.assertEquals("6", stageDesc.getStageFlowNodes().get(0).getId());
        Assert.assertEquals(jenkinsRule.jenkins.getDescriptorByType(ErrorStep.DescriptorImpl.class).getDisplayName(), stageDesc.getStageFlowNodes().get(0).getName());
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/execution/node/6/wfapi/describe", stageDesc.getStageFlowNodes().get(0).get_links().self.href);
        Assert.assertEquals("[5]", stageDesc.getStageFlowNodes().get(0).getParentNodes().toString());

        Assert.assertEquals(StatusExt.FAILED, stageDesc.getStageFlowNodes().get(0).getStatus()); // If flow continued, we succeeded
        Assert.assertEquals("my specific failure message", stageDesc.getStageFlowNodes().get(0).getError().getMessage());
        Assert.assertEquals("hudson.AbortException", stageDesc.getStageFlowNodes().get(0).getError().getType());
        validateChildNodeDescribeAPIs(stageDesc, webClient);
    }

    /** Exercise the describe APIs for a stage node */
    private void validateChildNodeDescribeAPIs(StageNodeExt stage, JenkinsRule.WebClient client) throws IOException, SAXException {
        JSONReadWrite jrw = new JSONReadWrite();
        if (stage.getStageFlowNodes() == null) {
            return;
        }
        for (AtomFlowNodeExt atom : stage.getStageFlowNodes()) {
            String ref = atom.get_links().self.href.replace("/jenkins/", ""); // Strip off relative path prefix
            String response = client.goTo(ref, "application/json").getWebResponse().getContentAsString();
            AtomFlowNodeExt describeAtomNode = jrw.fromString(response, AtomFlowNodeExt.class);

            Assert.assertEquals(atom.getDurationMillis(), describeAtomNode.getDurationMillis());
            if (atom.getError() != null) {
                Assert.assertEquals(atom.getError().getMessage(), describeAtomNode.getError().getMessage());
            }
            Assert.assertEquals(atom.getId(), describeAtomNode.getId());
            Assert.assertEquals(atom.getStatus(), describeAtomNode.getStatus());
            Assert.assertEquals(atom.getName(), describeAtomNode.getName());
            if (describeAtomNode.getParentNodes().size() != 0) { // Actually just a FlowNodeExt
                Assert.assertEquals(atom.getParentNodes(), describeAtomNode.getParentNodes());
            }
            Assert.assertEquals(atom.getPauseDurationMillis(), describeAtomNode.getPauseDurationMillis());
            Assert.assertEquals(atom.getStartTimeMillis(), describeAtomNode.getStartTimeMillis());

            // Fudge for cases where we get back a FlowNodeExt that isn't really an AtomFlowNodeExt
            if (describeAtomNode.get_links() != null && describeAtomNode.get_links().getLog() != null) {
                Assert.assertEquals(atom.get_links().getLog().href, describeAtomNode.get_links().getLog().href);
            }
        }

    }
}
