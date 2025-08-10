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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import hudson.model.TimeZoneProperty;
import hudson.model.User;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class WorkflowStageViewActionTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    private WebDriver driver;

    @Before
    public void setUp() {
        // Only run if ChromeDriver is available
        try {
            Class.forName("org.openqa.selenium.chrome.ChromeDriver");
        } catch (ClassNotFoundException e) {
            System.out.println("ChromeDriver not available, skipping Selenium timezone test.");
            Assume.assumeTrue(false);
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        driver = new ChromeDriver(options);
    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void test() throws Exception {
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "Noddy Job");

        job.setDefinition(new CpsFlowDefinition("" +
                "node {" +
                "   stage ('Build'); " +
                "   sh ('echo Building'); " +
                "   stage ('Test'); " +
                "   sh ('echo Testing'); " +
                "   stage ('Deploy'); " +
                "   sh ('echo Deploying'); " +
                "}", true));

        WorkflowRun build = job.scheduleBuild2(0).get();
        jenkinsRule.assertBuildStatusSuccess(build);

        HtmlPage page = jenkinsRule.createWebClient().goTo(job.getUrl());

        // Make sure the jobsTable is rendered in the page
        HtmlElement jobsTable = page.getFirstByXPath("//table[contains(@class,'jobsTable')]");
        Assert.assertNotNull(jobsTable);

        // Should have just one job
        List<HtmlElement> jobs = jobsTable.getByXPath(".//tr[contains(@class,'job')]");
        Assert.assertEquals(1, jobs.size());

        // That job should have 3 stages
        List<HtmlElement> jobStages = jobs.get(0).getByXPath(".//div[contains(@class,'stage-wrapper')]");
        Assert.assertEquals(3, jobStages.size());
    }

    @Test
    public void testTimezoneRespectedInStageStartTime() throws Exception {
        // Given a jenkins instance
        jenkinsRule.jenkins.setSecurityRealm(jenkinsRule.createDummySecurityRealm());
        // And a simple pipeline project with a successful execution
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "TZ Job");
        String pipelineScript = "node { stage ('Build'); sh ('echo Building'); }";
        System.out.println("Pipeline script for testTimezoneRespectedInStageStartTime():\n" + pipelineScript);
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun build = job.scheduleBuild2(0).get();
        jenkinsRule.assertBuildStatusSuccess(build);
        long startMillis = build.getStartTimeInMillis();
        Instant startInstant = Instant.ofEpochMilli(startMillis);
        // And a browser with a configured timezone
        String browserTz = (String)
                ((JavascriptExecutor) driver).executeScript("return Intl.DateTimeFormat().resolvedOptions().timeZone;");
        ZoneId browserZoneId = ZoneId.of(browserTz);
        // And some users with different timezone preferences
        class UserTZ {
            final String userId;
            final String tz;
            final ZoneId expectedZoneId;

            UserTZ(String userId, String tz, ZoneId expectedZoneId) {
                this.userId = userId;
                this.tz = tz;
                this.expectedZoneId = expectedZoneId;
            }
        }
        Collection<UserTZ> users = Arrays.asList(
                new UserTZ("a", "Australia/Sydney", ZoneId.of("Australia/Sydney")),
                new UserTZ("b", "America/New_York", ZoneId.of("America/New_York")),
                // Null or invalid timezone property fallback is browser timezone
                new UserTZ("c", null, browserZoneId),
                new UserTZ("d", "Invalid/Zone", browserZoneId));
        for (UserTZ utz : users) {
            User user = User.getById(utz.userId, true);
            if (utz.tz != null) {
                user.addProperty(new TimeZoneProperty(utz.tz));
            }
            user.save();
        }

        for (UserTZ utz : users) {
            // When the user logs in and access the job page
            driver.get(jenkinsRule.getURL().toString() + "login");
            driver.findElement(By.name("j_username")).sendKeys(utz.userId);
            driver.findElement(By.name("j_password")).sendKeys(utz.userId);
            driver.findElement(By.name("Submit")).click();
            WebDriverWait loginWait = new WebDriverWait(driver, Duration.ofSeconds(10));
            loginWait.until(d ->
                    d.findElements(By.xpath("//a[contains(@href, 'logout')]")).size() > 0);
            driver.get(jenkinsRule.getURL().toString() + job.getUrl());
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            String renderedDate = wait.until(driver1 -> {
                        List<WebElement> elements = driver1.findElements(
                                By.xpath("//div[contains(@class,'stage-start-time')]/div[@class='date']"));
                        return elements.isEmpty() ? null : elements.get(0);
                    })
                    .getText();
            String renderedTime = wait.until(driver1 -> {
                        List<WebElement> elements = driver1.findElements(
                                By.xpath("//div[contains(@class,'stage-start-time')]/div[@class='time']"));
                        return elements.isEmpty() ? null : elements.get(0);
                    })
                    .getText();

            // Then the rendered stage start date use the expected timezone
            ZoneId zoneId = utz.expectedZoneId;
            ZonedDateTime zdt = ZonedDateTime.ofInstant(startInstant, zoneId);
            String expectedMonth = zdt.getMonth().toString().substring(0, 1)
                    + zdt.getMonth().toString().substring(1, 3).toLowerCase();
            // NOTE toLocaleTimeString day: '2-digit'
            String expectedDay = String.format("%02d", zdt.getDayOfMonth());
            boolean matchesDate = renderedDate.contains(expectedMonth) && renderedDate.contains(expectedDay);
            Assert.assertTrue(
                    "Rendered date '" + renderedDate + "' should contain expected month '" + expectedMonth
                            + "' and day '" + expectedDay + "' for timezone " + zoneId + " (user " + utz.userId + ")",
                    matchesDate);
            // And the rendered stage start time use the expected timezone
            // NOTE toLocaleTimeString hour12: false / day: '2-digit'
            String expectedTime = String.format("%02d:%02d", zdt.getHour(), zdt.getMinute());
            Assert.assertTrue(
                    "Rendered time '" + renderedTime + "' should match expected time " + expectedTime + " for timezone "
                            + zoneId + " (user " + utz.userId + ")",
                    renderedTime.contains(expectedTime));
        }
    }
}
