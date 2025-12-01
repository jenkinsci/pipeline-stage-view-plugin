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
package com.cloudbees.workflow.ui;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import hudson.model.Item;
import jenkins.model.Jenkins;

/**
 * Abstract base class for running web based tests.
 * <p/>
 * Why not just use HtmlUnit?  Coz it's a pain wrt javascript and it doesn't support toLocaleDateString
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public abstract class AbstractWebDriverTest {

    protected WebDriver driver;

    @Before
    public void setUp() {
        // Only run if ChromeDriver is available
        try {
            Class.forName("org.openqa.selenium.chrome.ChromeDriver");
        } catch (ClassNotFoundException e) {
            Assume.assumeTrue("ChromeDriver not available, skipping Selenium timezone test.", false);
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(options);

        try {
            driver.get("data:text/html, <form id='f' onsubmit='document.body.textContent=\"ok\"; return false;'>"
                    + "<button id='b' type='submit'>Go</button></form>");
            driver.findElement(By.id("b")).click();
            try {
                new WebDriverWait(driver, Duration.ofSeconds(2))
                        .until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), "ok"));
            } catch (TimeoutException te) {
                Assume.assumeTrue("Chrome crashed or JS submit broken: " + te, false);
            }
        } catch (Exception e) {
            Assume.assumeTrue("Skipping test because Chrome crashed: " + e, false);
        }
    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    protected WebDriver getWebDriver() {
        return driver;
    }

    protected void moveMouseToElement(WebDriver webdriver, WebElement element) {
        Actions actions = new Actions(webdriver);
        actions.moveToElement(element).build().perform();
    }

    protected void clickOnElement(WebDriver webdriver, WebElement element) {
        Actions actions = new Actions(webdriver);
        actions.moveToElement(element).click().perform();
    }

    protected void moveMouseOffElement(WebDriver webdriver) {
        Actions actions = new Actions(webdriver);
        actions.moveToElement(webdriver.findElement(By.cssSelector("html"))).build().perform();
    }

    protected List<WebElement> waitForElementsAdded(WebElement inElement, String cssSelector) {
        List<WebElement> elements = Collections.emptyList();

        long start = System.currentTimeMillis();
        while (elements.isEmpty() && System.currentTimeMillis() < (start + 20000)) {
            elements = inElement.findElements(By.cssSelector(cssSelector));
        }

        if (elements.isEmpty()) {
            Assert.fail("Timed out waiting on elements matching CSS selector '" + cssSelector + "' to appear on the page.");
        }

        return elements;
    }

    protected List<WebElement> waitForElementsAdded(WebDriver webdriver, String cssSelector) {
        return waitForElementsAdded(webdriver.findElement(By.cssSelector("html")), cssSelector);
    }

    protected void waitForElementsRemoved(WebElement inElement, String cssSelector) {
        List<WebElement> elements = inElement.findElements(By.cssSelector(cssSelector));

        long start = System.currentTimeMillis();
        while (!elements.isEmpty() && System.currentTimeMillis() < (start + 20000)) {
            elements = inElement.findElements(By.cssSelector(cssSelector));
        }

        if (!elements.isEmpty()) {
            Assert.fail("Timed out waiting on elements matching CSS selector '" + cssSelector + "' to be removed from the page.");
        }
    }

    protected void waitForElementsRemoved(WebDriver webdriver, String cssSelector) {
        waitForElementsRemoved(webdriver.findElement(By.cssSelector("html")), cssSelector);
    }

    protected String getItemUrl(Jenkins jenkins, Item item) {
        return getJenkinsUrl(jenkins, item.getUrl());
    }

    protected String getJenkinsUrl(Jenkins jenkins, String itemUrl) {
        return jenkins.getRootUrl() + itemUrl;
    }
}
