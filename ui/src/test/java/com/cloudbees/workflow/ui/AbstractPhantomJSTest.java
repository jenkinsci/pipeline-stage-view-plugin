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

import hudson.model.Item;
import jenkins.model.Jenkins;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for running phantomJS based tests.
 * <p/>
 * Why not just use HtmlUnit?  Coz it's a pain wrt javascript.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public abstract class AbstractPhantomJSTest {

    private static DesiredCapabilities sCaps = new DesiredCapabilities();
    private WebDriver mDriver = null;

    @BeforeClass
    public static void configure() throws IOException {
        sCaps.setJavascriptEnabled(true);
        sCaps.setCapability("takesScreenshot", false);
        sCaps.setBrowserName("phantomjs");

        ArrayList<String> cliArgsCap = new ArrayList<String>();
        cliArgsCap.add("--web-security=false");
        cliArgsCap.add("--ssl-protocol=any");
        cliArgsCap.add("--ignore-ssl-errors=true");
        sCaps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, cliArgsCap);

        // Set LogLevel for GhostDriver
        sCaps.setCapability(PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_CLI_ARGS, new String[]{"--logLevel=DEBUG"});
    }

    @Before
    public void prepareDriver() {
        try {
            assertPhantomJSExecPathOK();
            mDriver = new PhantomJSDriver(sCaps);
        } catch (Exception e) {
            Assert.fail("Unable to create PhantomJS WebDriver.  PhantomJS must be installed on the machine executing the tests.  Exception: " + e.getMessage());
        }
    }

    protected WebDriver getWebDriver() {
        return mDriver;
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
        List<WebElement> elements = Collections.EMPTY_LIST;

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

    @After
    public void quitDriver() {
        if (mDriver != null) {
            mDriver.quit();
            mDriver = null;
        }
    }

    protected String getItemUrl(Jenkins jenkins, Item item) {
        return getJenkinsUrl(jenkins, item.getUrl());
    }

    protected String getJenkinsUrl(Jenkins jenkins, String itemUrl) {
        return jenkins.getRootUrl() + itemUrl;
    }

    private static void assertPhantomJSExecPathOK() {
        String phantomJsExePath = System.getProperty(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY);
        if (phantomJsExePath != null) {
            if (!(new File(phantomJsExePath).exists())) {
                System.out.println("***************************************************************************************");
                System.out.println(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY + " system property is set to '" + phantomJsExePath + "'.");
                System.out.println("No such file exists.  Test may be running in an IDE?");
                System.out.println("Removing system property in the hope that phantomjs can be found on the path.");
                System.out.println("***************************************************************************************");
                System.getProperties().remove(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY);
            } else {
                System.out.println(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY + " system property is set to '" + phantomJsExePath + "'.");
            }
        } else {
            System.out.println(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY + " system property not set.  Will try use the system path.");
        }
    }
}
