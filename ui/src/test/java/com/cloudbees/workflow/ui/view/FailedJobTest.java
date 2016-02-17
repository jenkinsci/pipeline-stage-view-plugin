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
import hudson.model.Result;
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

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class FailedJobTest extends AbstractPhantomJSTest {

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
                "   sh ('blah'); " +
                "}"));

        QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
        jenkinsRule.assertBuildStatus(Result.FAILURE, build.get());

        String jobUrl = getItemUrl(jenkinsRule.jenkins, job);
        webdriver.get(jobUrl);

        // Make sure the stage cell was marked as failed...
        List<WebElement> failedStageCells = webdriver.findElements(By.cssSelector(".stage-cell.FAILED .stage-wrapper"));
        Assert.assertEquals(1, failedStageCells.size());

        // Make the sure the stage-failed-popover widget was added to the cell
        WebElement failedStageCell = failedStageCells.get(0);
        List<WebElement> stageFailedPopovers = failedStageCell.findElements(By.cssSelector(".stage-failed-popover"));
        Assert.assertEquals(1, stageFailedPopovers.size());

        // Make sure that when we mouse over the failed stage cell we get a popup...
        moveMouseToElement(webdriver, failedStageCell);
        List<WebElement> popovers = waitForElementsAdded(webdriver, ".cbwf-popover");
//        System.out.println(webdriver.getPageSource());
        Assert.assertTrue(popovers.size() > 0);

        // Make sure the popover has what we expect...
        Assert.assertEquals("Failed with the following error(s)\n" +
                "Shell Script script returned exit code 127\n" +
                "See stage logs for more detail.\n" +
                "Logs", popovers.get(0).getText().trim());

        // Make sure the popover is removed once we move off it
        //moveMouseOffElement(webdriver);
        //waitForElementsRemoved(webdriver, ".cbwf-popover");
    }
}
