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

import com.cloudbees.workflow.flownode.FlowAnalyzer;
import com.cloudbees.workflow.flownode.FlowNodeUtil;
import com.gargoylesoftware.htmlunit.Page;
import hudson.model.queue.QueueTaskFuture;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.List;

/**
 * Test performance and behavior with gigantic flow graphs
 * @author svanoort
 */
@Ignore // For benchmark only
public class HugeFlowTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void test_giant_flow() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "GiantNode");
        String jobRunsUrl = job.getUrl() + "wfapi/runs/";

        // Will generate a stack overflow
        job.setDefinition(new CpsFlowDefinition("" +
                "stage 'all'\n" +
                "for (int i = 0; i < 9999; i++) {\n" +
                "  echo \"running #${i}\"\n" +
                "}"
        ));

        QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
        jenkinsRule.assertBuildStatusSuccess(build);

        String runsUrl = job.getLastBuild().getUrl();

        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();

        long startWalk = System.nanoTime();
        FlowGraphWalker w = new FlowGraphWalker(job.getLastBuild().getExecution());
        FlowNode root; // Keep a ref in memory
        for (FlowNode node : w) {
            if (node.getParents() == null || node.getParents().isEmpty()) {
                root = node;
            }
        }
        long endWalk = System.nanoTime();
        System.out.println("Walking time (ns): "+(endWalk-startWalk));

        long analyzeStart = System.nanoTime();
        List<FlowNode> nodes = FlowNodeUtil.getStageNodes(job.getBuilds().getLastBuild().getExecution());
        for (FlowNode f : nodes) {
            FlowNodeUtil.getStageExecDuration(f);
        }
        long analyzeEnd = System.nanoTime();
        System.out.println("Run time to create a run: " + (analyzeEnd - analyzeStart));

        analyzeStart = System.nanoTime();
        FlowAnalyzer an = new FlowAnalyzer(job.getLastBuild().getExecution());
        an.analyzeAll();
        analyzeEnd = System.nanoTime();
        System.out.println("Run time to run analysis: " + (analyzeEnd - analyzeStart));
        List<FlowAnalyzer.FlowSegment> stageEntries = an.getStages();

        long requestStart = System.nanoTime();
        Page runsPage = webClient.goTo(jobRunsUrl, "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();
        long requestEnd = System.nanoTime();

        System.out.println("Listing runs took "+(requestEnd-requestStart)+ " ns"); // Initially 1432371000 ns

        requestStart = System.nanoTime();
        Page jobDescribe = webClient.goTo(jobRunsUrl+"wfapi/describe", "application/json");
        jsonResponse = jobDescribe.getWebResponse().getContentAsString();
        requestEnd = System.nanoTime();
        System.out.println("Describing the job took "+(requestEnd-requestStart)+ " ns"); // Initially 72851000 ns

        requestStart = System.nanoTime();
        Page runDescribe = webClient.goTo(runsUrl+"wfapi/describe", "application/json"); // Initially 60449000 ns
        jsonResponse = runDescribe.getWebResponse().getContentAsString();
        requestEnd = System.nanoTime();
        System.out.println("Describing the run took "+(requestEnd-requestStart)+ " ns");

        requestStart = System.nanoTime();
        Page changesets = webClient.goTo(runsUrl + "wfapi/changesets", "application/json"); // Initially 18493000 ns
        jsonResponse = changesets.getWebResponse().getContentAsString();
        requestEnd = System.nanoTime();
        System.out.println("Getting the run changesets took "+(requestEnd-requestStart)+ " ns");

        requestStart = System.nanoTime();
        Page nodePage = webClient.goTo(job.getUrl()+"/1/execution/node/3/wfapi/describe", "application/json");
        jsonResponse = nodePage.getWebResponse().getContentAsString();
        requestEnd = System.nanoTime();
        System.out.println("Stage took "+(requestEnd-requestStart)+ " ns"); // Initially 10213394000 ns
    }
}
