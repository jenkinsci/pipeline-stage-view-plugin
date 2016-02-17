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

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class BuildArtifactsTest extends AbstractPhantomJSTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Ignore("remove phantomjs: https://trello.com/c/JpUg8S5z/159-get-rid-of-phantomjs-webdriver")
    @Test
    public void test() throws Exception {
        WebDriver webdriver = getWebDriver();

        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "Noddy Job");

        job.setDefinition(new CpsFlowDefinition("" +
                "node {" +
                "   stage ('Archiving'); " +
                "   sh('mkdir targs && echo hello > targs/hello1.txt && echo hello > targs/hello2.txt'); " +
                "   archive(includes: 'targs/*.txt'); " +
                "}"));

        QueueTaskFuture<WorkflowRun> build = job.scheduleBuild2(0);
        jenkinsRule.assertBuildStatusSuccess(build);

        String jobUrl = getItemUrl(jenkinsRule.jenkins, job);
        webdriver.get(jobUrl);

        // Look for the build artifacts popup button...
        WebElement buildArtifactsPopupBtn = webdriver.findElement(By.cssSelector(".build-artifacts-popup"));
        Assert.assertNotNull(buildArtifactsPopupBtn);

        // Move over the button to load the popover...
        moveMouseToElement(webdriver, buildArtifactsPopupBtn);

        // Look for and test the popover content...
        List<WebElement> buildArtifactsPopover = waitForElementsAdded(webdriver, ".cbwf-build-artifacts");
        Assert.assertEquals(1, buildArtifactsPopover.size());

        WebElement popover = buildArtifactsPopover.get(0);
        List<WebElement> artifacts = waitForElementsAdded(popover, ".artifact");
        Assert.assertEquals(2, artifacts.size());

        // TODO: Something strange here with selenium.
        // Can't seem to get the text of that element even though it is there if you sout the whole page.
        // isDisplayed on those anchor elements returns false for some reason, even though the parent elements
        // are "displayed".
        // Assert.assertEquals("hello1.txt", artifacts.get(0).findElement(By.cssSelector(".name")).getText());
        // Assert.assertEquals("hello2.txt", artifacts.get(1).findElement(By.cssSelector(".name")).getText());

        // Make sure it goes away once we move off the popover...
        moveMouseOffElement(webdriver);
        waitForElementsRemoved(webdriver, ".cbwf-build-artifacts");
    }
}
