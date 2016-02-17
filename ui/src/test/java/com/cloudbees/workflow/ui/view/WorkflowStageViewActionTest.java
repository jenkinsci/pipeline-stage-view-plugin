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
package com.cloudbees.workflow.ui.view;

import com.cloudbees.workflow.ui.AbstractPhantomJSTest;
import hudson.model.queue.QueueTaskFuture;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.openqa.selenium.phantomjs.PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class WorkflowStageViewActionTest extends AbstractPhantomJSTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Ignore("remove phantomjs: https://trello.com/c/JpUg8S5z/159-get-rid-of-phantomjs-webdriver")
    @Test
    public void test() throws Exception {
        WebDriver webdriver = getWebDriver();

        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "Noddy Job");

        job.setDefinition(new CpsFlowDefinition("" +
                "node {" +
                "   stage ('Build'); " +
                "   sh ('echo Building'); " +
                "   stage ('Test'); " +
                "   sh ('echo Testing'); " +
                "   stage ('Deploy'); " +
                "   sh ('echo Deploying'); " +
                "}"));

        QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
        jenkinsRule.assertBuildStatusSuccess(build);

        String jobUrl = getItemUrl(jenkinsRule.jenkins, job);
        webdriver.get(jobUrl);

//        System.out.println(webdriver.getPageSource());

        // Make sure the jobsTable is rendered in the page
        WebElement jobsTable = webdriver.findElement(By.className("jobsTable"));
        Assert.assertNotNull(jobsTable);

        // Check the totals are rendered
//        List<WebElement> stageWrappers = jobsTable.findElements(By.cssSelector(".totals .stage-wrapper"));
//        Assert.assertEquals(3, stageWrappers.size());

        // Should have just one job
        List<WebElement> jobs = jobsTable.findElements(By.cssSelector(".job"));
        Assert.assertEquals(1, jobs.size());

        // That job should have 3 stages
        List<WebElement> jobStages = jobs.get(0).findElements(By.cssSelector(".stage-wrapper"));
        Assert.assertEquals(3, jobStages.size());
    }
}
