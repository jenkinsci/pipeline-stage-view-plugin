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
import com.cloudbees.workflow.util.ModelUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import hudson.model.ParameterDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.input.InputAction;
import org.jenkinsci.plugins.workflow.support.steps.input.InputStepExecution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class PendingInputActionsExt {

    private String id;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String proceedText;
    private String message;
    private List<InputParameterDefExt> inputs;
    private String proceedUrl; // Not a rest endpoint YET, so not including in _links
    private String abortUrl; // Not a rest endpoint YET, so not including in _links
    private String redirectApprovalUrl;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProceedText() {
        return proceedText;
    }

    public void setProceedText(String proceedText) {
        this.proceedText = proceedText;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<InputParameterDefExt> getInputs() {
        return inputs;
    }

    public void setInputs(List<InputParameterDefExt> inputs) {
        this.inputs = inputs;
    }

    public String getProceedUrl() {
        return proceedUrl;
    }

    public void setProceedUrl(String proceedUrl) {
        this.proceedUrl = proceedUrl;
    }

    public String getAbortUrl() {
        return abortUrl;
    }

    public void setAbortUrl(String abortUrl) {
        this.abortUrl = abortUrl;
    }

    public String getRedirectApprovalUrl() {
        return redirectApprovalUrl;
    }

    public void setRedirectApprovalUrl(String redirectApprovalUrl) {
        this.redirectApprovalUrl = redirectApprovalUrl;
    }

    public static PendingInputActionsExt create(InputStepExecution inputStepExecution, WorkflowRun run) {
        PendingInputActionsExt inputActionExt = new PendingInputActionsExt();
        String inputId = inputStepExecution.getId();
        inputActionExt.setId(inputId);
        inputActionExt.setProceedText(inputStepExecution.getInput().getOk());
        inputActionExt.setMessage(inputStepExecution.getInput().getMessage());

        String runUrl = ModelUtil.getFullItemUrl(run.getUrl());
        inputActionExt.setInputs(getInputParams(inputId, run));
        inputActionExt.setProceedUrl(RunAPI.getInputStepSubmitUrl(run, inputId));
        inputActionExt.setAbortUrl(runUrl + "input/" + inputId + "/abort");
        inputActionExt.setRedirectApprovalUrl(runUrl + "input/");

        return inputActionExt;
    }


    private static List<InputParameterDefExt> getInputParams(String inputId, WorkflowRun run) {
        InputAction inputAction = run.getAction(InputAction.class);

        if (inputAction != null) {
            InputStepExecution execution = inputAction.getExecution(inputId);
            if (execution != null) {
                List<ParameterDefinition> inputParamDefs = execution.getInput().getParameters();
                List<InputParameterDefExt> inputParameters = new ArrayList<InputParameterDefExt>();

                for (ParameterDefinition inputParamDef : inputParamDefs) {
                    inputParameters.add(new InputParameterDefExt(inputParamDef));
                }

                return inputParameters;
            }
        }

        return Collections.emptyList();
    }

}
