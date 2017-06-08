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
import com.cloudbees.workflow.rest.external.AtomFlowNodeExt;
import com.cloudbees.workflow.rest.external.BuildArtifactExt;
import com.cloudbees.workflow.rest.external.ChangeSetExt;
import com.cloudbees.workflow.rest.external.ErrorExt;
import com.cloudbees.workflow.rest.external.JobExt;
import com.cloudbees.workflow.rest.external.RunExt;
import com.cloudbees.workflow.rest.external.StageNodeExt;
import com.cloudbees.workflow.rest.external.StatusExt;
import com.cloudbees.workflow.util.JSONReadWrite;
import com.gargoylesoftware.htmlunit.Page;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.queue.QueueTaskFuture;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

/**
 * Test the raw job/run APIs
 */
public class JobAndRunAPITest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void testBlockStage() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "Blocky job");

        job.setDefinition(new CpsFlowDefinition("" +
                "node {" +
                "   stage ('Build') { " +
                "     echo ('Building'); " +
                "   } \n" +
                "   stage ('Test') { " +
                "     echo ('Testing'); " +
                "   } \n" +
                "   stage ('Deploy') { " +
                "     writeFile file: 'file.txt', text:'content'; " +
                "     archive(includes: 'file.txt'); " +
                "     echo ('Deploying'); " +
                "   } \n" +
                "}"));

        QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
        jenkinsRule.assertBuildStatusSuccess(build);

        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();

        assertBasicJobInfoOkay(job, webClient);
        assertArtifactsEndpointOkay(job, webClient);
        assertChangesetsEndpointOkay(job, webClient);

        /** Verify the runs show right stages as per {@link #assertBasicRunInfoOkay(WorkflowJob, JenkinsRule.WebClient)} */
        String jobRunsUrl = job.getUrl() + "wfapi/runs?fullStages=true";
        Page runsPage = webClient.goTo(jobRunsUrl, "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();

        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        RunExt[] workflowRuns = jsonReadWrite.fromString(jsonResponse, RunExt[].class);

        Assert.assertEquals(1, workflowRuns.length);
        RunExt runExt = workflowRuns[0];
        assertRunInfoOkay(job, runExt, 6, 11, 16);

        /** Test the describe endpoint, similar to {@link #assertDescribeEndpointOkay(WorkflowJob, JenkinsRule.WebClient)} */
        String descUrl = job.getUrl() + "1/wfapi/describe";
        runsPage = webClient.goTo(descUrl, "application/json");
        jsonResponse = runsPage.getWebResponse().getContentAsString();
        RunExt workflowRun = jsonReadWrite.fromString(jsonResponse, RunExt.class);
        assertRunInfoOkay(job, workflowRun, 6, 11, 16);

        // Run another build and then test resultset narrowing using the 'since' query parameter
        build = job.scheduleBuild2(0);
        jenkinsRule.assertBuildStatusSuccess(build);
        assertSinceQueryParamOkay(job, webClient);
    }

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
                "     writeFile file: 'file.txt', text:'content'; " +
                "     archive(includes: 'file.txt'); " +
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

    @Test
    public void testEmptyStageHandling() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "EmptyStageJob");

        job.setDefinition(new CpsFlowDefinition("" +
                "stage 'empty'\n" +
                "stage 'nope' \n" +
                "echo \"I'm passing\"\n"));

        QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
        jenkinsRule.assertBuildStatus(Result.SUCCESS, build.get());

        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();

        assertBasicJobInfoOkay(job, webClient);

        String jobRunsUrl = job.getUrl() + "wfapi/runs?fullStages=true";
        Page runsPage = webClient.goTo(jobRunsUrl, "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();
        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        RunExt[] workflowRuns = jsonReadWrite.fromString(jsonResponse, RunExt[].class);
        Assert.assertEquals(1, workflowRuns.length);
        RunExt run = workflowRuns[0];

        Assert.assertEquals(2, run.getStages().size());
        StageNodeExt first = run.getStages().get(0);
        StageNodeExt second = run.getStages().get(1);
        Assert.assertEquals(0, first.getStageFlowNodes().size());
        Assert.assertEquals(1, second.getStageFlowNodes().size());
        assertStageInfoOkay(first, false);
        assertStageInfoOkay(second, true);
        Assert.assertTrue(first.getDurationMillis() > 0);
    }

    @Issue("JENKINS-40162")  // Zero duration for run
    @Test
    public void testDuration0EdgeCase() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "DurationBug");

        job.setDefinition(new CpsFlowDefinition("" +
                "echo 'hello guys'\n" +
                "stage 'one'\n" +
                "node {\n" +
                "    sh 'sleep 5'\n" +
                "}\n" +
                "stage 'two'\n" +
                "node {\n" +
                "    sh 'sleep 5'\n" +
                "}\n" +
                "stage 'three'\n" +
                "echo 'we are going to crash....'\n" +
                "throw new Exception(\"crash!!!\")\n"
                ));

        QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
        jenkinsRule.assertBuildStatus(Result.FAILURE, build.get());
        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();

        assertBasicJobInfoOkay(job, webClient);

        String jobRunsUrl = job.getUrl() + "wfapi/runs?fullStages=true";
        Page runsPage = webClient.goTo(jobRunsUrl, "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();
        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        RunExt[] workflowRuns = jsonReadWrite.fromString(jsonResponse, RunExt[].class);
        Assert.assertEquals(1, workflowRuns.length);
        RunExt run = workflowRuns[0];
        assertRunPassesSanity(build.get(), run, true);

        // Some extra sanity checks here just to ensure there's no deeper wackiness happening
        Assert.assertEquals(3, run.getStages().size());
        StageNodeExt first = run.getStages().get(0);
        StageNodeExt second = run.getStages().get(1);
        StageNodeExt third = run.getStages().get(1);
        Assert.assertEquals(1, first.getStageFlowNodes().size());
        Assert.assertEquals(1, second.getStageFlowNodes().size());
        Assert.assertEquals(1, third.getStageFlowNodes().size());
        assertStageInfoOkay(first, true);
        assertStageInfoOkay(second, true);
        assertStageInfoOkay(third, true);
        Assert.assertTrue(first.getDurationMillis() > 0);
    }

    @Test
    public void testErrorHandling() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "Failing Job");

        job.setDefinition(new CpsFlowDefinition("" +
                "stage \"pass\"\n" +
                "echo \"I'm passing\"\n" +
                "\n" +
                "stage \"catch-an-error\"\n" +
                "catchError {\n" +
                "   error \"You won't see me?\"\n" +
                "}\n" +
                "\n" +
                "stage \"and now keep passing\"\n" +
                "echo \"still passing\""));

        QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
        jenkinsRule.assertBuildStatus(Result.FAILURE, build.get());

        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();

        assertBasicJobInfoOkay(job, webClient);

        String jobRunsUrl = job.getUrl() + "wfapi/runs?fullStages=true";
        Page runsPage = webClient.goTo(jobRunsUrl, "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();
        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        RunExt[] workflowRuns = jsonReadWrite.fromString(jsonResponse, RunExt[].class);

        Assert.assertEquals(1, workflowRuns.length);
        RunExt runExt = workflowRuns[0];
        Assert.assertEquals(StatusExt.FAILED, runExt.getStatus());
        assertRunPassesSanity(build.get(), runExt, true);
        List<StageNodeExt> stages = runExt.getStages();
        Assert.assertEquals(3, stages.size());

        for (StageNodeExt st : runExt.getStages()) {
            assertStageInfoOkay(st, true);
        }

        Assert.assertEquals(StatusExt.SUCCESS, stages.get(0).getStatus());
        Assert.assertEquals(StatusExt.SUCCESS, stages.get(1).getStatus());
        Assert.assertEquals(StatusExt.FAILED, stages.get(2).getStatus());

        StageNodeExt finalStage = stages.get(2);
        AtomFlowNodeExt finalNode = finalStage.getStageFlowNodes().get(finalStage.getStageFlowNodes().size()-1);
        Assert.assertEquals(StatusExt.SUCCESS, finalNode.getStatus());
    }



    /** Checks handling of one buggy FlowEndNode case */
    private void assertTimingHandlesBuggyFlowEndNode(WorkflowJob job, JenkinsRule.WebClient webClient) throws Exception {
        WorkflowRun run = job.getLastBuild();
        FlowNode lastNode = run.getExecution().getCurrentHeads().get(0);
        List<Action> actions = lastNode.getActions();
        TimingAction timing = lastNode.getAction(TimingAction.class);

        // Remove timing info from last node (root cause of one set of bugs) and flush the cache
        int timingIndex = actions.indexOf(timing);
        actions.remove(timingIndex);
        FlowNodeUtil.CacheExtension ext = FlowNodeUtil.CacheExtension.all().get(0);
        ext.getRunCache().invalidateAll();

        Page runsPage = webClient.goTo(run.getUrl() + "/wfapi/describe", "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();
        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        RunExt runData = jsonReadWrite.fromString(jsonResponse, RunExt.class);
        assertRunInfoOkay(job, runData, 5, 7, 9);
    }

    private void assertBasicJobInfoOkay(WorkflowJob job, JenkinsRule.WebClient webClient) throws IOException, SAXException {
        String jobRunsUrl = job.getUrl() + "wfapi/describe/";
        Page runsPage = webClient.goTo(jobRunsUrl, "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();

//        System.out.println(jsonResponse);

        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        JobExt jobExt = jsonReadWrite.fromString(jsonResponse, JobExt.class);

        Assert.assertEquals(job.getName(), jobExt.getName());
        Assert.assertEquals(1, jobExt.getRunCount());
        Assert.assertEquals("/jenkins/" + job.getUrl() + "wfapi/describe", jobExt.get_links().self.href);
        Assert.assertEquals("/jenkins/" + job.getUrl() + "wfapi/runs", jobExt.get_links().getRuns().href);
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

        assertRunPassesSanity(job.getLastBuild(), runExt, false);
        assertRunInfoOkay(job, runExt, 5, 7, 9);
    }

    private void assertRunPassesSanity(WorkflowRun myRun, RunExt runExt, boolean testStageChildren) {
        long stageRunTime = 0;
        long stagePauseTime = 0;
        if (runExt.getStages() != null && runExt.getStages().size() > 0) {
            for (StageNodeExt stage : runExt.getStages()) {
                assertStageInfoOkay(stage, testStageChildren);
                stageRunTime += stage.getDurationMillis();
                stagePauseTime += stage.getPauseDurationMillis();
            }
        }
        Assert.assertTrue("Run has zero duration!", runExt.getDurationMillis() > 0);
        Assert.assertTrue(String.format("Run duration (%d) ms is shorter than sum of stage runtimes (%d ms), this is insane!",
                    runExt.getDurationMillis(), stageRunTime),
                runExt.getDurationMillis() >= stageRunTime);
        Assert.assertEquals(stagePauseTime, runExt.getPauseDurationMillis());
        Assert.assertEquals(myRun.getId(), runExt.getId());
        Assert.assertEquals(myRun.getDisplayName(), runExt.getName());
        Assert.assertEquals(myRun.getStartTimeInMillis(), runExt.getStartTimeMillis());
    }

    private void assertDescribeEndpointOkay(WorkflowJob job, JenkinsRule.WebClient webClient) throws IOException, SAXException {
        String descUrl = job.getUrl() + "1/wfapi/describe";
        Page runsPage = webClient.goTo(descUrl, "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();

//        System.out.println(jsonResponse);

        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        RunExt workflowRun = jsonReadWrite.fromString(jsonResponse, RunExt.class);

        assertRunInfoOkay(job, workflowRun, 5, 7, 9);
    }

    private void assertStageInfoOkay(StageNodeExt stageNodeExt, boolean mustHaveChildNodes) {
        Assert.assertTrue("Stage has duration >0!", stageNodeExt.getDurationMillis() > 0);
        Assert.assertTrue("Stage has pause duration >= 0!", stageNodeExt.getPauseDurationMillis() >= 0);
        Assert.assertTrue("Stage has startTimeMillis >= 0!", stageNodeExt.getStartTimeMillis() > 0);

        if (mustHaveChildNodes) {
            Assert.assertNotNull(stageNodeExt.getStageFlowNodes());
            Assert.assertTrue(stageNodeExt.getStageFlowNodes().size() > 0);
        }

        if (stageNodeExt.getStageFlowNodes() == null) {
            return;
        }
        for (AtomFlowNodeExt childNode : stageNodeExt.getStageFlowNodes()) {
            Assert.assertTrue(childNode.getName() != null && !(childNode.getName().isEmpty()));
            Assert.assertTrue(childNode.getId() != null && !(childNode.getId().isEmpty()));
            // Handle me: errorExt
            ErrorExt err = childNode.getError();
            if (err != null) {
                Assert.assertNotNull(err.getMessage());
                Assert.assertNotNull(err.getType());
            }
            Assert.assertNotNull(childNode.getStatus());

            Assert.assertTrue("Start times should always be > 0 after 1970", childNode.getStartTimeMillis() > 0);
            Assert.assertTrue("Durations should always be >= 0", childNode.getDurationMillis() >= 0);
            Assert.assertTrue("Pause Durations should always be >= 0", childNode.getPauseDurationMillis() >= 0);
        }
    }

    private void assertRunInfoOkay(WorkflowJob job, RunExt runExt,  int stageid1, int stageId2, int stageId3) {
        assertRunPassesSanity(job.getLastBuild(), runExt, false);

        Assert.assertEquals("#1", runExt.getName());
        Assert.assertEquals(StatusExt.SUCCESS, runExt.getStatus());
        Assert.assertEquals("/jenkins/" + job.getUrl() + "1/wfapi/describe", runExt.get_links().self.href);
        Assert.assertEquals("/jenkins/"+job.getUrl()+"1/wfapi/artifacts", runExt.get_links().getArtifacts().href);
        Assert.assertEquals(3, runExt.getStages().size());

        // Stage 1 test
        StageNodeExt stage = runExt.getStages().get(0);
        Assert.assertEquals("Build", stage.getName());
        Assert.assertEquals("/jenkins/"+job.getUrl()+"1/execution/node/"+stageid1+"/wfapi/describe", stage.get_links().self.href);
        Assert.assertEquals(StatusExt.SUCCESS, stage.getStatus());
        if (stage.getStageFlowNodes() != null) {
            Assert.assertEquals(1, stage.getStageFlowNodes().size());
        }

        // Stage 2 test
        stage = runExt.getStages().get(1);
        Assert.assertEquals("Test", stage.getName());
        Assert.assertEquals("/jenkins/"+job.getUrl()+"1/execution/node/"+stageId2+"/wfapi/describe", stage.get_links().self.href);
        Assert.assertEquals(StatusExt.SUCCESS, stage.getStatus());
        if (stage.getStageFlowNodes() != null) {
            Assert.assertEquals(1, stage.getStageFlowNodes().size());
        }

        // Stage 3 test
        stage = runExt.getStages().get(2);
        Assert.assertEquals("Deploy", stage.getName());
        Assert.assertEquals("/jenkins/"+job.getUrl()+"1/execution/node/"+stageId3+"/wfapi/describe", stage.get_links().self.href);
        Assert.assertEquals(StatusExt.SUCCESS, stage.getStatus());
        if (stage.getStageFlowNodes() != null) {
            Assert.assertEquals(3, stage.getStageFlowNodes().size());
        }

        List<FlowNode> nodes = FlowNodeUtil.getStageNodes(job.getLastBuild().getExecution());
        Assert.assertEquals(runExt.getStages().size(), nodes.size());
        for (int i=0; i<runExt.getStages().size(); i++) {
            StageNodeExt st = runExt.getStages().get(i);
            // assertStageInfoOkay is called in the assertRunPassesSanity above
            Assert.assertEquals(st.getId(), nodes.get(i).getId());

            if (stage.getStageFlowNodes() != null) {
                List<FlowNode> childNodes = FlowNodeUtil.getStageNodes(nodes.get(i));
                Assert.assertEquals(st.getStageFlowNodes().size(), childNodes.size());
                for (int j=0; j<childNodes.size(); j++) {
                    Assert.assertEquals(st.getStageFlowNodes().get(j).getId(), childNodes.get(j).getId());
                }
            }
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
        Assert.assertEquals("file.txt", buildArtifactExts[0].getName());
        Assert.assertEquals("file.txt", buildArtifactExts[0].getPath());
        Assert.assertEquals("/jenkins/"+job.getUrl()+"1/artifact/file.txt", buildArtifactExts[0].getUrl());
        Assert.assertEquals(7, buildArtifactExts[0].getSize());
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

    @Test
    @Issue("JENKINS-33700")
    public void test_unstable_basic_flow() throws Exception {
        // Succeeds but custom-marked as unstable
        WorkflowJob passUnstable = jenkinsRule.jenkins.createProject(WorkflowJob.class, "SafelyUnstable");
        passUnstable.setDefinition(new CpsFlowDefinition("" +
                "stage 'first'\n" +
                "echo 'works'\n" +
                "stage 'second'\n" +
                "currentBuild.result = 'UNSTABLE'\n" +
                "echo 'ran things'\n" +
                "stage 'end'\n" +
                "echo 'done'"
        ));
        QueueTaskFuture<WorkflowRun> build = passUnstable.scheduleBuild2(0);
        jenkinsRule.assertBuildStatus(Result.UNSTABLE, build.get());
        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();
        RunExt[] workflowRuns = getRuns(passUnstable, webClient, "wfapi/runs?fullStages=true");

        // Confirm that in the case of success with overriding, all stages are marked as unstable
        Assert.assertEquals(1, workflowRuns.length);
        RunExt run = workflowRuns[0];
        assertRunPassesSanity(build.get(), run, true);
        Assert.assertEquals(StatusExt.UNSTABLE, run.getStatus());
        Assert.assertEquals(3, run.getStages().size());
        Assert.assertEquals(StatusExt.UNSTABLE, run.getStages().get(0).getStatus());
        Assert.assertEquals(StatusExt.UNSTABLE, run.getStages().get(1).getStatus());
        Assert.assertEquals(StatusExt.UNSTABLE, run.getStages().get(2).getStatus());
    }

    @Test
    @Issue("JENKINS-33700")
    public void test_unstable_try_catch_flow() throws Exception {
        // Error generated, but caught and custom-marked as unstable
        WorkflowJob failUnstable = jenkinsRule.jenkins.createProject(WorkflowJob.class, "DangerouslyUnstable");
        failUnstable.setDefinition(new CpsFlowDefinition("" +
                "stage 'safe'\n" +
                "echo 'okay'\n" +
                "stage 'warning'\n" +
                "try { \n" +
                "    error('danger will robinson') \n" +
                "} \n" +
                "catch (Exception e) {\n" +
                "    currentBuild.result = 'UNSTABLE'    \n" +
                "    echo 'eaten error!' \n" +
                "}"));

        QueueTaskFuture<WorkflowRun> build = failUnstable.scheduleBuild2(0);
        jenkinsRule.assertBuildStatus(Result.UNSTABLE, build.get());
        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();

        // Confirms that unstable & status override works with try/catch per JENKINS-34212 as well
        RunExt[] workflowRuns = getRuns(failUnstable, webClient, "wfapi/runs?fullStages=true");
        Assert.assertEquals(1, workflowRuns.length);
        RunExt run = workflowRuns[0];
        assertRunPassesSanity(build.get(), run, true);
        Assert.assertEquals(StatusExt.UNSTABLE, run.getStatus());
        Assert.assertEquals(2, run.getStages().size());
        Assert.assertEquals(StatusExt.UNSTABLE, run.getStages().get(0).getStatus());
        Assert.assertEquals(StatusExt.UNSTABLE, run.getStages().get(1).getStatus());
    }

    @Test
    @Issue("JENKINS-34212")
    public void test_basic_try_catch_flow() throws Exception {
        // Verify that caught errors do not cause a stage to be flagged as erroring, as complex as a the game of thrones
        WorkflowJob oneStageCatch = jenkinsRule.jenkins.createProject(WorkflowJob.class, "One_Stage_Catch");
        oneStageCatch.setDefinition(new CpsFlowDefinition("" + // Tyrion in King's Landing
                "stage 'Game of Thrones'\n" +
                "try {\n" +
                "    error('Trusted Grand Maester Pycelle')\n" +
                "} catch (Exception e) {\n" +
                "    echo 'It was a trap - Tyriowned!'\n" +
                "}"
        ));
        QueueTaskFuture<WorkflowRun> build = oneStageCatch.scheduleBuild2(0);
        jenkinsRule.assertBuildStatusSuccess(build);
        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();

        // Single stage should reflect final build state
        RunExt[] workflowRuns = getRuns(oneStageCatch, webClient, "wfapi/runs?fullStages=true");
        Assert.assertEquals(1, workflowRuns.length);
        RunExt run = workflowRuns[0];
        assertRunPassesSanity(build.get(), run, true);
        Assert.assertEquals(StatusExt.SUCCESS, run.getStatus());
        Assert.assertEquals(1, run.getStages().size());
        Assert.assertEquals(StatusExt.SUCCESS, run.getStages().get(0).getStatus());
    }

    @Test
    @Issue("JENKINS-34212")
    public void test_multistage_try_catch_flow() throws Exception {
        // Multiple stages where first error is caught, there's an ErrorAction on first stage but it can be safely ignored
        WorkflowJob multiStageCatch = jenkinsRule.jenkins.createProject(WorkflowJob.class, "Multi_Stage_catch");
        multiStageCatch.setDefinition(new CpsFlowDefinition("" + //The One True King, Stannis
                "stage 'Battle of the Blackwater'\n" +
                "try {\n" +
                "    error('Failed to watch out for Tywin and his army')\n" +
                "} catch (Exception e) {\n" +
                "    echo 'Ran away to Dragonstone'\n" +
                "}\n" +
                "stage 'Flee to the North'\n" +
                "echo 'Helped defeat the Wildlings'"
        ));
        QueueTaskFuture<WorkflowRun> build = multiStageCatch.scheduleBuild2(0);
        jenkinsRule.assertBuildStatusSuccess(build);
        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();

        // Confirm that try/catch was handled: all stages should be marked successful
        RunExt[] workflowRuns = getRuns(multiStageCatch, webClient, "wfapi/runs?fullStages=true");
        Assert.assertEquals(1, workflowRuns.length);
        RunExt run = workflowRuns[0];
        assertRunPassesSanity(build.get(), run, true);
        Assert.assertEquals(StatusExt.SUCCESS, run.getStatus());
        Assert.assertEquals(2, run.getStages().size());
        Assert.assertEquals(StatusExt.SUCCESS, run.getStages().get(0).getStatus());
        Assert.assertEquals(StatusExt.SUCCESS, run.getStages().get(1).getStatus());
    }

    @Test
    @Issue("JENKINS-36523")
    public void test_try_catch_override_to_fail() throws Exception {
        // One stage where error is caught, but job still fails by explicitly setting status
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "One_Stage_Catch");

        job.setDefinition(new CpsFlowDefinition("" +
                "stage 'Catch and fail'\n" +
                "try {\n" +
                "    error('All the things are broken')\n" +
                "} catch (Exception ex) {\n" +
                "    echo 'Stage failed'\n" +
                "    currentBuild.result = 'FAILURE'\n" +
                "}"
        ));

        QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
        jenkinsRule.assertBuildStatus(Result.FAILURE, build.get());

        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();
        JSONReadWrite jsonReadWrite = new JSONReadWrite();

        // Confirm that in the case of success with overriding, all stages are marked as unstable
        String jsonResponse = webClient.goTo(job.getUrl()+"wfapi/runs?fullStages=true", "application/json")
                .getWebResponse().getContentAsString();
        RunExt[] workflowRuns = jsonReadWrite.fromString(jsonResponse, RunExt[].class);
        Assert.assertEquals(1, workflowRuns.length);
        RunExt run = workflowRuns[0];
        assertRunPassesSanity(build.get(), run, true);
        Assert.assertEquals(StatusExt.FAILED, run.getStatus());
        Assert.assertEquals(1, run.getStages().size());
        Assert.assertEquals(StatusExt.FAILED, run.getStages().get(0).getStatus());
    }
}
