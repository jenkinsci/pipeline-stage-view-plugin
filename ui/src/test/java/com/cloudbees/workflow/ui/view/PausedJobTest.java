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

import java.time.Duration;
import java.util.List;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.input.InputAction;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.cloudbees.workflow.ui.AbstractWebDriverTest;
import com.cloudbees.workflow.ui.Util;

import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class PausedJobTest extends AbstractWebDriverTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void test() throws Exception {
        WebDriver webdriver = getWebDriver();

        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "Noddy Job");

        job.setDefinition(
                new CpsFlowDefinition("node { stage ('Build'); echo ('build'); input 'Is the build okay?' }", true));

        QueueTaskFuture<WorkflowRun> q = job.scheduleBuild2(0);
        WorkflowRun b = q.getStartCondition().get();
        CpsFlowExecution e = (CpsFlowExecution) b.getExecutionPromise().get();

        while (b.getAction(InputAction.class) == null) {
            e.waitForSuspension();
        }

        String jobUrl = getItemUrl(jenkinsRule.jenkins, job);
        webdriver.get(jobUrl);

        // Make sure the stage cell was marked as pending input...
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        List<WebElement> pausedStageCells = wait.until(driver1 -> {
            List<WebElement> elements =
                    driver1.findElements(By.cssSelector(".stage-cell.PAUSED_PENDING_INPUT .stage-wrapper"));
            return elements.isEmpty() ? null : elements;
        });
        Assert.assertEquals(1, pausedStageCells.size());

        // Move over the paused build and check for the popup...
        moveMouseToElement(webdriver, pausedStageCells.get(0));
        List<WebElement> inputRequiredPopovers = waitForElementsAdded(webdriver, ".cbwf-popover .run-input-required");
        Assert.assertEquals(1, inputRequiredPopovers.size());

        // Check the popup content...
        WebElement inputRequiredPopover = inputRequiredPopovers.get(0);
        WebElement message = inputRequiredPopover.findElement(By.className("caption"));
        Assert.assertEquals("Is the build okay?", message.getText());

        // Click on the proceed button
        WebElement proceedBtn = inputRequiredPopover.findElement(By.className("proceed-button"));
        clickOnElement(webdriver, proceedBtn);

        // Wait for the build to complete successfully
        Util.waitForBuildCount(job, 1, Result.SUCCESS);

        // Click on the proceed button and wait for the popup to disappear
        waitForElementsRemoved(webdriver, ".cbwf-popover .run-input-required");
    }
}
