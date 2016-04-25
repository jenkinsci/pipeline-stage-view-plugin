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

import com.cloudbees.workflow.rest.endpoints.RunAPI;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class BuildArtifactExt {

    private String id;
    private String name;
    private String path;
    private String url; // Not a rest endpoint so not including in _links
    private long size;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public static BuildArtifactExt create(Run<WorkflowJob, WorkflowRun>.Artifact artifact, WorkflowRun run) {
        BuildArtifactExt inputActionExt = new BuildArtifactExt();

        inputActionExt.setId(artifact.getTreeNodeId());
        inputActionExt.setName(artifact.getDisplayPath());

        // DisplayPath does some custom processing to handle collisions, but isn't set if we have more than
        // Run.LIST_CUTOFF artifacts - since we're just using this for display, relativePath is good enough
        String nameCandidate = artifact.getDisplayPath();
        inputActionExt.setName(
                (nameCandidate != null) ? nameCandidate : artifact.relativePath
        );
        inputActionExt.setPath(artifact.getHref());
        inputActionExt.setUrl(RunAPI.getArtifactUrl(run, artifact));
        inputActionExt.setSize(artifact.getFileSize());

        return inputActionExt;
    }
}
