/*
 * Copyright (C) 2013 CloudBees Inc.
 *
 * All rights reserved.
 */
package com.cloudbees.workflow.ui.view;

import hudson.Extension;
import hudson.model.Action;
import jenkins.model.TransientActionFactory;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

/**
 * {@link WorkflowJob} ui extension point {@link Action}.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class WorkflowStageViewAction implements Action {

    public final WorkflowJob target;

    private WorkflowStageViewAction(WorkflowJob job) {
        this.target = job;
    }

    @Override
    public String getDisplayName() {
        return Messages.full_stage_view();
    }

    @Override
    public String getUrlName() {
        return "workflow-stage";
    }

    @Override
    public String getIconFileName() {
        return "package.png";
    }

    @Extension
    public static class Factory extends TransientActionFactory<WorkflowJob> {

        @Override
        public Class<WorkflowJob> type() {
            return WorkflowJob.class;
        }

        @Override
        public Collection<? extends Action> createFor(WorkflowJob target) {
            return Collections.singleton(new WorkflowStageViewAction(target));
        }
    }

    public String getTimeZone() {
        return System.getProperty("user.timezone");
    }

    // Time zone offset right now in HH:MM format
    public String getTimeZoneOffset() {
        TimeZone tz = TimeZone.getTimeZone(getTimeZone());
        int offsetTotalMin = tz.getOffset(new Date().getTime()) / (1000*60) ;
        int absOffsetMin = Math.abs(offsetTotalMin);
        int offsetHours = absOffsetMin/60;
        int offsetRealMin = absOffsetMin-(offsetHours*60);
        return ((offsetTotalMin>0) ? '+' : '-') + offsetHours + ":" + offsetRealMin;
    }
}
