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

import com.cloudbees.workflow.util.ModelUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class ChangeSetExt {

    private String kind;
    private int commitCount;
    private List<Commit> commits;
    private String consoleUrl; // Not a rest endpoint so not including in _links


    /** Allows user to disable Jenkins user lookup for commit authors
     * By setting System Property com.cloudbees.workflow.rest.external.ChangeSetExt.resolveCommitAuthors to 'false'
     * This is a workaround for JENKINS-35484 where user lookup encounters issues */
    private static boolean resolveCommitAuthors() {
        String prop = System.getProperty(ChangeSetExt.class.getName()+".resolveCommitAuthors");
        return (StringUtils.isEmpty(prop)|| Boolean.parseBoolean(prop));
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public int getCommitCount() {
        return commitCount;
    }

    public void setCommitCount(int commitCount) {
        this.commitCount = commitCount;
    }

    public List<Commit> getCommits() {
        return commits;
    }

    public void setCommits(List<Commit> commits) {
        this.commits = commits;
    }

    public String getConsoleUrl() {
        return consoleUrl;
    }

    public void setConsoleUrl(String consoleUrl) {
        this.consoleUrl = consoleUrl;
    }

    public static class Commit {
        private String commitId;
        private String commitUrl;
        private String authorJenkinsId;
        private String message;
        private long timestamp;
        private String consoleUrl; // Not a rest endpoint so not including in _links

        public String getCommitId() {
            return commitId;
        }

        public void setCommitId(String commitId) {
            this.commitId = commitId;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getCommitUrl() {
            return commitUrl;
        }

        public void setCommitUrl(String commitUrl) {
            this.commitUrl = commitUrl;
        }

        public String getAuthorJenkinsId() {
            return authorJenkinsId;
        }

        public void setAuthorJenkinsId(String authorJenkinsId) {
            this.authorJenkinsId = authorJenkinsId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public String getConsoleUrl() {
            return consoleUrl;
        }

        public void setConsoleUrl(String consoleUrl) {
            this.consoleUrl = consoleUrl;
        }
    }

    public static boolean hasChanges(WorkflowRun run) {
        for (ChangeLogSet<? extends hudson.scm.ChangeLogSet.Entry> changeset : run.getChangeSets()) {
            if (!changeset.isEmptySet()) {
                // there's some changes :)
                return true;
            }
        }

        return false;
    }

    public static ChangeSetExt create(ChangeLogSet<? extends ChangeLogSet.Entry> changeset, WorkflowRun run) {
        ChangeSetExt changeSetExt = new ChangeSetExt();
        changeSetExt.mapFields(changeset, run);
        return changeSetExt;
    }

    // TODO: https://trello.com/c/ENpHLEYD/132-vis-scm-changelog-v2

    protected void mapFields(ChangeLogSet<? extends ChangeLogSet.Entry> changeset, WorkflowRun run) {
        Iterator<? extends ChangeLogSet.Entry> iterator = changeset.iterator();

        RepositoryBrowser<?> repoBrowser = changeset.getBrowser();

        setKind(changeset.getKind());
        setCommits(new ArrayList<Commit>());
        setConsoleUrl(getRunUrl(run) + "changes");

        while(iterator.hasNext()) {
            ChangeLogSet.Entry entry = iterator.next();
            Commit commit = new Commit();

            String repoUrl = getCommitUrl(repoBrowser, entry);

            getCommits().add(commit);
            commit.setCommitId(entry.getCommitId());
            commit.setCommitUrl(repoUrl);
            commit.setMessage(entry.getMsg());
            commit.setAuthorJenkinsId(resolveCommitAuthors() ? entry.getAuthor().getFullName() : "");
            commit.setTimestamp(entry.getTimestamp());

            if (commit.getTimestamp() > -1) {
                if (commit.getTimestamp() > 999999999L && commit.getTimestamp() < 999999999999L) {
                    // looks as though this timestamp is in seconds.  Change to millis
                    commit.setTimestamp(commit.getTimestamp() * 1000);
                }
            }

            commit.setConsoleUrl(getConsoleUrl() + "#" + commit.getCommitId());
        }

        setCommitCount(getCommits().size());
    }

    protected String getRunUrl(WorkflowRun run) {
        return ModelUtil.getFullItemUrl(run.getUrl());
    }

    static <T extends ChangeLogSet.Entry> String getCommitUrl(RepositoryBrowser<T> repoBrowser, ChangeLogSet.Entry entry) {
        if (repoBrowser == null) {
            return null;
        }
        try {
            URL changeSetLink = repoBrowser.getChangeSetLink((T) entry);
            if (changeSetLink == null) {
                return null;
            }
            return changeSetLink.toString();
        } catch (IOException e) {
            return null;
        }
    }
}
