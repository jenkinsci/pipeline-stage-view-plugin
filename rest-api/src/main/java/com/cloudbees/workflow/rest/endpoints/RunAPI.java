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
package com.cloudbees.workflow.rest.endpoints;

import com.cloudbees.workflow.rest.AbstractWorkflowRunActionHandler;
import com.cloudbees.workflow.rest.external.BuildArtifactExt;
import com.cloudbees.workflow.rest.external.ChangeSetExt;
import com.cloudbees.workflow.rest.external.PendingInputActionsExt;
import com.cloudbees.workflow.rest.external.RunExt;
import com.cloudbees.workflow.util.ModelUtil;
import com.cloudbees.workflow.util.ServeJson;
import hudson.Extension;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.input.InputAction;
import org.jenkinsci.plugins.workflow.support.steps.input.InputStepExecution;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;

/**
 * API Action handler to return a single WorkflowJob run.
 * <p>
 * Bound to {@code ${{rootURL}/job/<jobname>/<runId>/wfapi/*}}
 * </p>
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@Extension
public class RunAPI extends AbstractWorkflowRunActionHandler {

    public static String getUrl(WorkflowRun run) {
        return ModelUtil.getFullItemUrl(run.getUrl()) + URL_BASE + "/";
    }

    public static String getDescribeUrl(WorkflowRun run) {
        return getUrl(run) + "describe";
    }

    public static String getChangeSetsUrl(WorkflowRun run) {
        return getUrl(run) + "changesets";
    }

    public static String getPendingInputActionsUrl(WorkflowRun run) {
        return getUrl(run) + "pendingInputActions";
    }

    public static String getNextPendingInputActionUrl(WorkflowRun run) {
        return getUrl(run) + "nextPendingInputAction";
    }

    public static String getArtifactsUrl(WorkflowRun run) {
        return getUrl(run) + "artifacts";
    }

    public static String getInputStepSubmitUrl(WorkflowRun run, String inputId) {
        return getUrl(run) + "inputSubmit?inputId=" + inputId;
    }

    public static String getArtifactUrl(WorkflowRun run, Run<WorkflowJob, WorkflowRun>.Artifact artifact) {
        return ModelUtil.getFullItemUrl(run.getUrl()) + "artifact/" + artifact.getHref();
    }

    @Restricted(DoNotUse.class) // WebMethod
    @ServeJson
    public RunExt doIndex() {
        return doDescribe();
    }

    @Restricted(DoNotUse.class) // WebMethod
    @ServeJson
    public RunExt doDescribe() {
        return RunExt.create(getRun()).createWrapper();
    }

    @Restricted(DoNotUse.class) // WebMethod
    @ServeJson
    public List<ChangeSetExt> doChangesets() {
        List<ChangeSetExt> changeSetExts = new ArrayList<ChangeSetExt>();
        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changesets = getRun().getChangeSets();

        for (ChangeLogSet<? extends ChangeLogSet.Entry> changeset : changesets) {
            changeSetExts.add(ChangeSetExt.create(changeset, getRun()));
        }

        return changeSetExts;
    }

    @Restricted(DoNotUse.class) // WebMethod
    @ServeJson
    public List<PendingInputActionsExt> doPendingInputActions() {
        List<PendingInputActionsExt> pendingInputActions = new ArrayList<PendingInputActionsExt>();
        InputAction inputAction = getRun().getAction(InputAction.class);

        if (inputAction != null) {
            List<InputStepExecution> executions = inputAction.getExecutions();
            if (executions != null && !executions.isEmpty()) {
                for (InputStepExecution inputStepExecution : executions) {
                    pendingInputActions.add(PendingInputActionsExt.create(inputStepExecution, getRun()));
                }
            }
        }

        return pendingInputActions;
    }

    @Restricted(DoNotUse.class) // WebMethod
    @ServeJson
    public PendingInputActionsExt doNextPendingInputAction() {
        InputAction inputAction = getRun().getAction(InputAction.class);

        if (inputAction != null) {
            List<InputStepExecution> executions = inputAction.getExecutions();
            if (executions != null && !executions.isEmpty()) {
                for (InputStepExecution inputStepExecution : executions) {
                    if (!inputStepExecution.isSettled()) {
                        return PendingInputActionsExt.create(inputStepExecution, getRun());
                    }
                }
            }
        }

        return null;
    }

    @Restricted(DoNotUse.class) // WebMethod
    @ServeJson
    public List<BuildArtifactExt> doArtifacts() {
        List<BuildArtifactExt> artifactExts = new ArrayList<BuildArtifactExt>();
        List<Run<WorkflowJob, WorkflowRun>.Artifact> artifacts = getRun().getArtifacts();

        if (artifacts != null && !artifacts.isEmpty()) {
            for (Run<WorkflowJob, WorkflowRun>.Artifact artifact : artifacts) {
                artifactExts.add(BuildArtifactExt.create(artifact, getRun()));
            }
        }

        return artifactExts;
    }

    @Restricted(DoNotUse.class) // WebMethod
    @RequirePOST
    @ServeJson
    public void doInputSubmit(@QueryParameter String inputId) throws ServletException {
        InputAction inputAction = getRun().getAction(InputAction.class);

        if (inputAction == null) {
            throw new ServletException("Error processing Input Submit request. This Run instance does not" +
                    " have an InputAction.");
        }

        InputStepExecution execution = inputAction.getExecution(inputId);
        if (execution == null) {
            // Note that InputStep normalizes the input ID it is given as part of the
            // input flow def (capitalizes the first char). InputAction.getExecution(id) (used above)
            // does not perform the same normalization on the id it is given, assuming
            // the supplied id is pre-normalized (e.g. came from a call to InputStep.getId()).
            // If the id is coming from a different source (and is not pre-normalized), this
            // assumption obviously fails, as will the call to InputAction.getExecution(id).
            throw new ServletException(String.format("Error processing Input Submit request. This Run instance does not" +
                    " have an Input with an ID of '%s'. The input ID may not be pre-normalized appropriately.", inputId));
        }

        try {
            // Pass the request off to the InputStepExecution, allowing it to process input data.
            execution.doProceed(Stapler.getCurrentRequest());
        } catch (Exception e) {
            throw new ServletException("Error processing Input Submit request.", e);
        }
    }
}
