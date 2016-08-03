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
package com.cloudbees.workflow.rest.external;

import com.cloudbees.workflow.rest.endpoints.JobAPI;
import com.cloudbees.workflow.rest.hal.Link;
import com.cloudbees.workflow.rest.hal.Links;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Predicate;
import hudson.util.Iterators;
import hudson.util.RunList;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class JobExt {

    /**
     * Max number of runs per page. Pagination not yet supported.
     */
    public static final int MAX_RUNS_PER_JOB = Integer.getInteger(JobExt.class.getName()+".maxRunsPerJob", 10);

    private JobLinks _links;
    private String name;
    private int runCount;

    public JobLinks get_links() {
        return _links;
    }

    public void set_links(JobLinks _links) {
        this._links = _links;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRunCount() {
        return runCount;
    }

    public void setRunCount(int runCount) {
        this.runCount = runCount;
    }

    public static final class JobLinks extends Links {
        private Link runs;

        public Link getRuns() {
            return runs;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public void setRuns(Link runs) {
            this.runs = runs;
        }
    }

    public static JobExt create(WorkflowJob job) {
        JobExt jobExt = new JobExt();

        jobExt.set_links((JobLinks) new JobLinks().initSelf(JobAPI.getDescribeUrl(job)));
        jobExt.get_links().setRuns(Link.newLink(JobAPI.getRunsUrl(job)));
        jobExt.setName(job.getName());
        jobExt.setRunCount(countRuns(job));

        return jobExt;
    }

    private static int countRuns(WorkflowJob job) {
        int count = 0;

        // RunList.size() is deprecated, so iterating to count them.
        RunList<WorkflowRun> runs = job.getBuilds();
        for (WorkflowRun run : runs) {
            count++;
        }

        return count;
    }

    public static List<RunExt> create(RunList<WorkflowRun> runs) {
        return create(runs, null);
    }

    public static List<RunExt> create(RunList<WorkflowRun> runs, String since) {
        return create(runs, since, false);
    }

    public static List<RunExt> create(RunList<WorkflowRun> runs, String since, boolean fullStages) {
        if (since != null) {
            since = since.trim();
            if (since.length() == 0) {
                since = null;
            }
        }

        // Limit the output to everything before the run named "since"
        RunList<WorkflowRun> filtered = runs.limit(MAX_RUNS_PER_JOB);
        ArrayList<RunExt> output = new ArrayList<RunExt>();
        for (WorkflowRun r : filtered) {
            if (since != null && since.equals(r.getDisplayName())) {
                break;
            }
            output.add((fullStages) ? RunExt.create(r) : RunExt.create(r).createWrapper());
        }
        return output;
    }
}
