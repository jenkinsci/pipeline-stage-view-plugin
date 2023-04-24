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

import com.cloudbees.workflow.rest.external.RunExt;
import com.cloudbees.workflow.rest.external.StageNodeExt;
import com.cloudbees.workflow.rest.external.StatusExt;
import com.cloudbees.workflow.util.JSONReadWrite;
import com.gargoylesoftware.htmlunit.Page;
import hudson.model.queue.QueueTaskFuture;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class ParallelStepTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void test_success_flow() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "Noddy Job");

        String script = "node {" +
                "  stage ('Stage 1') {" +
                "  parallel( " +
                "       a: { " +
                "           echo('echo a'); " + // ID=10
                "       }, " +
                "       b: { " +
                "           echo('echo b'); " + // ID=12
                "       } " +
                "  )};" +
                "  stage ('Stage 2') {" +
                "  echo('done..')}" +
                "}";

        // System.out.println(script);
        job.setDefinition(new CpsFlowDefinition(script, true));

        QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
        build.waitForStart();
        jenkinsRule.assertBuildStatusSuccess(build);

        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();

        String jobRunsUrl = job.getUrl() + "wfapi/runs/";
        Page runsPage = webClient.goTo(jobRunsUrl, "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();

        // System.out.println(jsonResponse);
        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        RunExt[] workflowRuns = jsonReadWrite.fromString(jsonResponse, RunExt[].class);

        // Check the run response... should be 2 stages
        Assert.assertEquals(1, workflowRuns.length);
        Assert.assertEquals(2, workflowRuns[0].getStages().size());
        Assert.assertEquals("Stage 1", workflowRuns[0].getStages().get(0).getName());
        Assert.assertEquals("Stage 2", workflowRuns[0].getStages().get(1).getName());

        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/execution/node/6/wfapi/describe", workflowRuns[0].getStages().get(0).get_links().self.href);
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/execution/node/19/wfapi/describe", workflowRuns[0].getStages().get(1).get_links().self.href);

        Page stageDescription = webClient.goTo("job/Noddy%20Job/1/execution/node/6/wfapi/describe", "application/json");
        jsonResponse = stageDescription.getWebResponse().getContentAsString();

        StageNodeExt stage1Desc = jsonReadWrite.fromString(jsonResponse, StageNodeExt.class);
        Assert.assertEquals(2, stage1Desc.getStageFlowNodes().size());
        Assert.assertEquals("11", stage1Desc.getStageFlowNodes().get(0).getId());
        Assert.assertEquals("Print Message", stage1Desc.getStageFlowNodes().get(0).getName());
        Assert.assertEquals("13", stage1Desc.getStageFlowNodes().get(1).getId());
        Assert.assertEquals("Print Message", stage1Desc.getStageFlowNodes().get(1).getName());

        stageDescription = webClient.goTo("job/Noddy%20Job/1/execution/node/19/wfapi/describe", "application/json");
        jsonResponse = stageDescription.getWebResponse().getContentAsString();

        StageNodeExt stage2Desc = jsonReadWrite.fromString(jsonResponse, StageNodeExt.class);
        Assert.assertEquals(1, stage2Desc.getStageFlowNodes().size());
        Assert.assertEquals("20", stage2Desc.getStageFlowNodes().get(0).getId());
        Assert.assertEquals("Print Message", stage2Desc.getStageFlowNodes().get(0).getName());
    }

    @Test
    public void test_stages_order_parallel_completed() throws Exception {
        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();

        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "Not A Sequential Job");
        String jobRunsUrl = job.getUrl() + "wfapi/runs/";

        String script = "node() {" +
                "  stage('SerialStartStage') {" +
                "    echo('SerialStartStage');" +
                "  };" +
                "  Map<String, Closure> parallelStages = [:];" +
                "  for (int i = 1; i <= 3; i++) {" +
                "    String stageName = 'ParallelStage' + i;" +
                "    parallelStages[stageName] = {" +
                "      stage(stageName) {" +
                "        int time = 1 + (i%2);" +
                "        echo(stageName + ':' + time);" +
                "        sleep(time);" + // ParallelStage2 will end before 1 and 3, it should not change the order 1, 2, 3 enforced by com.cloudbees.workflow.rest.external.RunExt#compareStageNodeExt in com.cloudbees.workflow.rest.external.RunExt#createNew
                "      };" +
                "    };" +
                "  };" +
                "  parallel parallelStages;" +
                "  stage('SerialEndStage') {" +
                "    echo('SerialEndStage');" +
                "  };" +
                "};";
        job.setDefinition(new CpsFlowDefinition(script, true));

        QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
        build.waitForStart();

        // Test while running
        RunExt runExt = getRunExt(jobRunsUrl);
        Assert.assertEquals(StatusExt.IN_PROGRESS, runExt.getStatus());
        while (runExt.getStages().size() < 4) {
            runExt = getRunExt(jobRunsUrl);
        }

        jenkinsRule.assertBuildStatusSuccess(build);

        // Test when completed
        runExt = getRunExt(jobRunsUrl);
        Assert.assertEquals(StatusExt.SUCCESS, runExt.getStatus());
        Assert.assertEquals(5, runExt.getStages().size());
        Assert.assertEquals("SerialStartStage", runExt.getStages().get(0).getName());
        Assert.assertEquals("ParallelStage1", runExt.getStages().get(1).getName());
        Assert.assertEquals("ParallelStage2", runExt.getStages().get(2).getName());
        Assert.assertEquals("ParallelStage3", runExt.getStages().get(3).getName());
        Assert.assertEquals("SerialEndStage", runExt.getStages().get(4).getName());
    }

    @Test
    public void test_stages_order_nested_sequential_completed() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "Not A Sequential Job");
        String jobRunsUrl = job.getUrl() + "wfapi/runs/";

        String script = "def branches = [" +
            "    'branch1': branch(1)," +
            "    'branch2': branch(2)" +
            "];" +
            "def branch(int index) {" +
            "    return {" +
            "        def delta = (1-((env.BUILD_NUMBER.toInteger()+index)%2))*2;" + // if build number is even the stages of one branch sleep less 2s less, if build number is odd the stages of the other branch sleep 2s less
            "        stage('branch' + index + '-stage1') {" +
            "            sleep(5+delta);" +
            "        };" +
            "        stage('branch' + index + '-stage2') {" +
            "            sleep(5+delta);" +
            "        };" +
            "    };" +
            "};" +
            "node(){" +
            "    parallel branches;" +
            "};";

        job.setDefinition(new CpsFlowDefinition(script, true));

        QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
        jenkinsRule.assertBuildStatusSuccess(build);

        // Test when completed
        RunExt runExt1 = getRunExt(jobRunsUrl);
        Assert.assertEquals(StatusExt.SUCCESS, runExt1.getStatus());
        Assert.assertEquals(4, runExt1.getStages().size());
        Assert.assertEquals("branch1-stage1", runExt1.getStages().get(0).getName());
        Assert.assertEquals("branch1-stage2", runExt1.getStages().get(1).getName());
        Assert.assertEquals("branch2-stage1", runExt1.getStages().get(2).getName());
        Assert.assertEquals("branch2-stage2", runExt1.getStages().get(3).getName());

        build = job.scheduleBuild2(0);
        jenkinsRule.assertBuildStatusSuccess(build);
        RunExt[] runExts = getRunExts(jobRunsUrl);
        Assert.assertEquals(2, runExts.length);
        runExt1 = runExts[0];
        RunExt runExt2 = runExts[1];
        Assert.assertEquals(StatusExt.SUCCESS, runExts[1].getStatus());
        Assert.assertEquals(4, runExt1.getStages().size());
        Assert.assertEquals(runExt1.getStages().size(), runExt2.getStages().size());
        Assert.assertEquals(runExt1.getStages().get(0).getName(), runExt2.getStages().get(0).getName());
        Assert.assertEquals(runExt1.getStages().get(1).getName(), runExt2.getStages().get(1).getName());
        Assert.assertEquals(runExt1.getStages().get(2).getName(), runExt2.getStages().get(2).getName());
        Assert.assertEquals(runExt1.getStages().get(3).getName(), runExt2.getStages().get(3).getName());
    }

    private RunExt getRunExt(String jobRunsUrl) throws Exception {
        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();
        String jsonResponse = webClient.goTo(jobRunsUrl, "application/json").getWebResponse().getContentAsString();
        RunExt[] runExts = new JSONReadWrite().fromString(jsonResponse, RunExt[].class);

        Assert.assertEquals(1, runExts.length);
        return runExts[0];
    }

    private RunExt[] getRunExts(String jobRunsUrl) throws Exception {
        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();
        String jsonResponse = webClient.goTo(jobRunsUrl, "application/json").getWebResponse().getContentAsString();
        return new JSONReadWrite().fromString(jsonResponse, RunExt[].class);
    }
}
