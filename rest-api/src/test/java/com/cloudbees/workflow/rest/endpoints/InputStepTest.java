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
import com.cloudbees.workflow.rest.external.PendingInputActionsExt;
import com.cloudbees.workflow.rest.external.RunExt;
import com.cloudbees.workflow.rest.external.StatusExt;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.input.InputAction;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.InputStream;
import java.net.URLEncoder;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class InputStepTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void test_basic() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "Noddy Job");

        job.setDefinition(new CpsFlowDefinition("" +
                "node {" +
                "   stage ('Build'); " +
                "   echo ('step1'); " +
                "   sleep (1); " +
                "   input(message: 'Is the build okay?') " +
                "   error ('break'); " +
                "}"));

        // schedule the workflow and then wait for it to enter
        // the pending input state
        job.scheduleBuild2(0);
        Util.waitForBuildCount(job, 1);
        WorkflowRun run = job.getFirstBuild();
        Util.waitForBuildPendingInput(run);

        // get the runs for the workflow (should only be 1).
        RunExt[] workflowRuns = Util.getJSON(job.getUrl() + "wfapi/runs/", RunExt[].class, jenkinsRule);

        // run status should be PAUSED_PENDING_INPUT
        Assert.assertEquals(1, workflowRuns.length);
        Assert.assertEquals("#1", workflowRuns[0].getName());
        Assert.assertEquals(StatusExt.PAUSED_PENDING_INPUT, workflowRuns[0].getStatus());

        // check the pendingInputActions link
        String pendingIAs = workflowRuns[0].get_links().getPendingInputActions().href;
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/wfapi/pendingInputActions", pendingIAs);
        PendingInputActionsExt[] inputActionsExts = Util.getJSON(pendingIAs, PendingInputActionsExt[].class, jenkinsRule);
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/wfapi/inputSubmit?inputId=Ef95dd500ae6ed3b27b89fb852296d12", inputActionsExts[0].getProceedUrl());
        Assert.assertEquals("/jenkins/job/Noddy%20Job/1/input/Ef95dd500ae6ed3b27b89fb852296d12/abort", inputActionsExts[0].getAbortUrl());

        // Workflow is paused at the input now. POST an empty inputs JSON to the "inputSubmit" url.
        JSONObject inputs = new JSONObject();
        JSONArray inputNVPs = new JSONArray();
        inputs.put("parameter", inputNVPs);
        String inputSubmitParams = "json=" + URLEncoder.encode(inputs.toString(), "UTF-8");

        // Trigger the build to proceed
        Util.postToJenkins(jenkinsRule.jenkins, inputActionsExts[0].getProceedUrl(), inputSubmitParams, "application/x-www-form-urlencoded;charset=UTF-8");
        // Last 'sh' step should fail...
        Util.waitForBuildCount(job, 1, Result.FAILURE);

        // Once we're done... duration should not be increasing...
        workflowRuns = Util.getJSON(job.getUrl() + "wfapi/runs/", RunExt[].class, jenkinsRule);
        long stageDurationT1 = workflowRuns[0].getStages().get(0).getDurationMillis();
        Thread.sleep(1000);
        workflowRuns = Util.getJSON(job.getUrl() + "wfapi/runs/", RunExt[].class, jenkinsRule);
        long stageDurationT2 = workflowRuns[0].getStages().get(0).getDurationMillis();
        Assert.assertTrue(stageDurationT1 == stageDurationT2);
    }

    @Test
    public void test_with_params() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "Job-With-Inputs");

        InputStream sampleFlowRes = getClass().getResourceAsStream("sample-flow.groovy");
        String sampleFlow = IOUtils.toString(sampleFlowRes);
        job.setDefinition(new CpsFlowDefinition(sampleFlow));

        // Start the job and wait for it to pause at the input action
        QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
        WorkflowRun run = build.getStartCondition().get();
        CpsFlowExecution flowExecution = (CpsFlowExecution) run.getExecutionPromise().get();
        while (run.getAction(InputAction.class) == null) {
            flowExecution.waitForSuspension();
        }

        // Workflow is paused at the input now. POST the inputs JSON to the "inputSubmit" url.
        JSONObject inputs = new JSONObject();
        JSONArray inputNVPs = new JSONArray();

        addNVP(inputNVPs, "Run test suites?", false);
        addNVP(inputNVPs, "Enter some text", "Hello World");
        addNVP(inputNVPs, "Enter a password", "guess-again");
        addNVP(inputNVPs, "Take your pick", "Choice 2");

        // The input NVPs need to be added to a JSON Object that contains a single
        // property named "parameter". Then we encode that into a post queuery parameter
        // named "json". Clear as mud!!
        inputs.put("parameter", inputNVPs);

        String inputSubmitUrl = run.getUrl() + "wfapi/inputSubmit?inputId=Run-test-suites";
        String inputSubmitParams = "json=" + URLEncoder.encode(inputs.toString(), "UTF-8");
        Util.postToJenkins(jenkinsRule.jenkins, inputSubmitUrl, inputSubmitParams, "application/x-www-form-urlencoded;charset=UTF-8");

        // Wait for the run to complete successfully
        Util.waitForBuildCount(job, 1, Result.SUCCESS);

        // Check the logs to make sure the submit worked. When it works properly, the inputs
        // get bound into an "outcome" object that gets returned to the workflow from the step.
        // The test flow just echos the values from the outcome.
        jenkinsRule.assertLogContains("P1: false", run);
        jenkinsRule.assertLogContains("P2: Hello World", run);
        jenkinsRule.assertLogContains("P3: guess-again", run);
        jenkinsRule.assertLogContains("P4: Choice 2", run);
    }

    private void addNVP(JSONArray inputNVPs, String name, Object value) {
        JSONObject nvp = new JSONObject();
        nvp.put("name", name);
        nvp.put("value", value);
        inputNVPs.add(nvp);
    }

    // TODO: Add tests that interact more with the UI.
    // We can do this once the Jenkins Core test-harness dep is new enough to bring in the new/unforked HtmlUnit.
    // The forked version of HtmlUnit in older Jenkins can't test this coz it's using jQuery etc.
    // We have some tests in JavaScript code.

}
