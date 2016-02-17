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

import com.cloudbees.workflow.rest.external.RunExt;
import com.cloudbees.workflow.util.JSONReadWrite;
import com.gargoylesoftware.htmlunit.Page;
import hudson.model.Result;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.jvnet.hudson.test.JenkinsRule;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class Util {

    private static JSONReadWrite jsonReadWrite = new JSONReadWrite();

    private Util() {
    }

    public static int postToJenkins(Jenkins jenkins, String url) throws IOException {
        return postToJenkins(jenkins, url, null, "multipart/form-data");
    }
    public static int postToJenkins(Jenkins jenkins, String url, String content, String contentType) throws IOException {
        String jenkinsUrl = jenkins.getRootUrl();
        URL urlObj = new URL(jenkinsUrl + url.replace("/jenkins/job/", "job/"));
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();

        try {
            conn.setRequestMethod("POST");

            if (contentType != null) {
                conn.setRequestProperty("Content-Type", contentType);
            }
            if (content != null) {
                byte[] bytes = content.getBytes(Charset.forName("UTF-8"));

                conn.setDoOutput(true);

                conn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
                final OutputStream os = conn.getOutputStream();
                try {
                    os.write(bytes);
                    os.flush();
                } finally {
                    os.close();
                }
            }

            return conn.getResponseCode();
        } finally {
            conn.disconnect();
        }
    }

    public static void waitForBuildPendingInput(WorkflowRun run) throws InterruptedException {
        long start = System.currentTimeMillis();

        while(!RunExt.isPendingInput(run)) {
            if (System.currentTimeMillis() > start + 120000) {
                Assert.fail("Timed out waiting on build to enter pending input state.");
            }
            Thread.sleep(200);
        }
    }

    public static void waitForBuildCount(WorkflowJob job, int numBuilds) throws InterruptedException {
        waitForBuildCount(job, numBuilds, null);
    }

    public static void waitForBuildCount(WorkflowJob job, int numBuilds, Result status) throws InterruptedException {
        long start = System.currentTimeMillis();

        while(countBuilds(job, status) < numBuilds) {
            // 2m is a long timeout but it seems as though it can actually take a fair bit of time for resumed
            // builds to complete.  Don't want the build randomly failing.
            if (System.currentTimeMillis() > start + 120000) {
                Assert.fail("Timed out waiting on build count to get to " + numBuilds);
            }
            Thread.sleep(200);
        }
    }

    public static int countBuilds(WorkflowJob job) {
        return countBuilds(job, null);
    }
    public static int countBuilds(WorkflowJob job, Result status) {
        RunList<WorkflowRun> builds = job.getNewBuilds();
        Iterator<WorkflowRun> iterator = builds.iterator();
        int numBuilds = 0;

        while (iterator.hasNext()) {
            WorkflowRun build = iterator.next();
            Result buildRes = build.getResult();
            if (status == null || buildRes == status) {
                numBuilds++;
            }
        }

        return numBuilds;
    }

    public static String removeRootUrl(String url) {
        return url.replace("/jenkins/", "");
    }

    public static <T> T getJSON(String url, Class<T> to, JenkinsRule jenkinsRule) throws IOException, SAXException {
        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();

        if (url.startsWith("/jenkins/job")) {
            url = url.substring("/jenkins/".length());
        }

        Page runsPage = webClient.goTo(url, "application/json");
        String jsonResponse = runsPage.getWebResponse().getContentAsString();

        return jsonReadWrite.fromString(jsonResponse, to);
    }

}
