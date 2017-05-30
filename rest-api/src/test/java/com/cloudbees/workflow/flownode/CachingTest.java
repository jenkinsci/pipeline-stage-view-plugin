/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
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
package com.cloudbees.workflow.flownode;

import com.cloudbees.workflow.rest.external.RunExt;
import com.google.common.cache.Cache;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Tests the improved run cache invalidation routines
 */
public class CachingTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void deletionTest() throws Exception {
        Cache<String, RunExt> cache = FlowNodeUtil.CacheExtension.all().get(0).getRunCache();

        // Create one job
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "BlinkinJob");
        job.setDefinition(new CpsFlowDefinition("" +
                "stage 'first' \n" +
                "echo 'done' "
        ));
        WorkflowRun build = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0));
        RunExt r = RunExt.create(build);
        String runKey = build.getExternalizableId();
        Assert.assertEquals(r, cache.getIfPresent(runKey));

        // Control: this one won't be touched
        WorkflowJob job2 = jenkinsRule.jenkins.createProject(WorkflowJob.class, "StableJob");
        job2.setDefinition(new CpsFlowDefinition("" +
                "stage 'second' \n" +
                "echo 'done' "
        ));
        WorkflowRun build2 = jenkinsRule.assertBuildStatusSuccess(job2.scheduleBuild2(0));
        RunExt r2 = RunExt.create(build2);
        String runKey2 = build2.getExternalizableId();
        Assert.assertEquals(r2, cache.getIfPresent(runKey2));

        // Check we still have the jobs cached appropriately
        job.delete();
        Assert.assertNull("Cache should be removed for deleted key", cache.getIfPresent(runKey));
        Assert.assertEquals("Non-deleted jobs should still be cached", r2, cache.getIfPresent(runKey2));
    }

    @Test
    public void renameTest() throws Exception {
        Cache<String, RunExt> cache = FlowNodeUtil.CacheExtension.all().get(0).getRunCache();

        // Create one job
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "BlinkinJob");
        job.setDefinition(new CpsFlowDefinition("" +
                "stage 'first' \n" +
                "echo 'done'"
        ));
        WorkflowRun build = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkinsRule.assertBuildStatusSuccess(build);
        RunExt r = RunExt.create(build);
        String runKey = build.getExternalizableId();
        Assert.assertEquals(r, cache.getIfPresent(runKey));

        // Control: this one won't be touched
        WorkflowJob job2 = jenkinsRule.jenkins.createProject(WorkflowJob.class, "StableJob");
        job2.setDefinition(new CpsFlowDefinition("" +
                "stage 'second' \n" +
                "echo 'done'"
        ));
        WorkflowRun build2 = jenkinsRule.assertBuildStatusSuccess(job2.scheduleBuild2(0));
        RunExt r2 = RunExt.create(build2);
        String runKey2 = build2.getExternalizableId();
        Assert.assertEquals(r2, cache.getIfPresent(runKey2));

        // Check we still have the jobs cached appropriately
        job.renameTo("NewName");
        String newJobKey = build.getExternalizableId();
        Assert.assertNull("Cache entry should be removed for renamed job", cache.getIfPresent(runKey));
        Assert.assertEquals("Non-renamed jobs should still be cached", r2, cache.getIfPresent(runKey2));
        Assert.assertNull("Cache entry should not be present with new job name", cache.getIfPresent(newJobKey));
    }
}
