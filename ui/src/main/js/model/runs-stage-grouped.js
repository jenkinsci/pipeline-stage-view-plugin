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

var time = require('../util/time');
var jqProxy = require('../jQuery');
var jobHistoryListener = require('./job-history-listener');

/**
 * Code for producing a model containing Workflow Job runs grouped by matching stage names.
 * <p/>
 * Multiple groups will occur where stages have been reordered, renamed or inserted.
 */

var maxRunsPerRunGroup = 5;

exports.getModelData = function (callback) {
    var mvcContext = this;
    var jobUrl = mvcContext.requiredAttr('objectUrl');

    jobHistoryListener.listen(jobUrl, function (jobRunsData) {
        if (!jobRunsData) {
            // TODO: come up with an error passing scheme
            return callback(undefined);
        }

        var jobRunGroups = mashupRunData(jobRunsData);

        // This callback can get called multiple times.  The first time it is called is for the initial data load.
        // Subsequent calls are for when the model changes. We only want to call the getModelData callback once
        // (the initial data load).  After that (for model changes), we fire a 'modelChange' event in the context
        // and anything that's listening can handle it.
        if (callback) {
            callback(jobRunGroups);
            callback = undefined; // blank it so we don't call it again
        } else {
            mvcContext.modelChange(jobRunGroups);
        }
    });
}

exports.setMaxRunsPerRunGroup = function (max) {
    maxRunsPerRunGroup = max;
}

function mashupRunData(jobRunsData) {
    var $ = jqProxy.getJQuery();
    var jobRunGroups = {
        runs: [].concat(jobRunsData)
    };

    // Total up timings etc
    addStageTotals(jobRunGroups);

    // Add end times before applying the template....
    addEndTimes(jobRunGroups);

    // Sort the runs into groups based on the stage names
    // and the number of runs
    createRunGroups(jobRunGroups);

    return jobRunGroups;
}

function getStageData(stageName, runGroup) {
    if (runGroup && runGroup.stageData) {
        for (var i = 0; i < runGroup.stageData.length; i++) {
            var stageData = runGroup.stageData[i];
            if (stageData.name === stageName) {
                return stageData;
            }
        }
    }
    return undefined;
}

function addStageTotals(runGroup) {
    if (runGroup.stageData === undefined) {
        runGroup.stageData = [];
    }

    if (runGroup.runs === undefined || runGroup.runs.length === 0) {
        return;
    }

    runGroup.numRuns = 0;
    runGroup.numStages = 0;
    runGroup.durationMillis = 0;
    runGroup.durationMillisNoPause = 0;
    runGroup.avgDurationMillis = 0;
    runGroup.avgDurationMillisNoPause = 0;

    // Maybe add this to the model?
    var endToEndRuns = 0;

    // Calc top level run and stage timings...
    for (var i = 0; i < runGroup.runs.length; i++) {
        var run = runGroup.runs[i];

        run.durationMillisNoPause = run.durationMillis - run.pauseDurationMillis;

        // For the top level run timings, only use successfully completed runs.
        if (run.status === 'SUCCESS') {
            runGroup.numRuns++;

            // When calculating run time averages, only include runs where every stage has been executed,
            // otherwise the times get skewed. We could get fancier here in time.
            if (run.stages && run.stages.length > 0 && run.stages[0].status !== 'NOT_EXECUTED') {
                endToEndRuns++;
                runGroup.durationMillis += run.durationMillis;
                runGroup.durationMillisNoPause += run.durationMillisNoPause;
                if (endToEndRuns > 0) {
                    runGroup.avgDurationMillis = Math.floor(runGroup.durationMillis / endToEndRuns);
                    runGroup.avgDurationMillisNoPause = Math.floor(runGroup.durationMillisNoPause / endToEndRuns);
                } else {
                    runGroup.avgDurationMillis = 0;
                    runGroup.avgDurationMillisNoPause = 0;
                }
            }
        }

        if (run.stages) {
            for (var ii = 0; ii < run.stages.length; ii++) {
                var stage = run.stages[ii];

                runGroup.numStages++;

                if (stage.status !== 'NOT_EXECUTED') {
                    var stageData = getStageData(stage.name, runGroup);

                    if (!stageData) {
                        stageData = {name: stage.name};
                        runGroup.stageData.push(stageData);
                    }

                    if (stageData.runs === undefined) {
                        stageData.runs = 0;
                        stageData.durationMillis = 0;
                        stageData.durationMillisNoPause = 0;
                        stageData.avgDurationMillis = 0;
                    }

                    stage.durationMillisNoPause = stage.durationMillis - stage.pauseDurationMillis;

                    // Only include stage times in calculations if the run was successful
                    if (run.status === 'SUCCESS') {
                        stageData.runs++;
                        stageData.durationMillis += stage.durationMillis;
                        stageData.durationMillisNoPause += stage.durationMillisNoPause;
                        stageData.avgDurationMillis = Math.floor(stageData.durationMillis / stageData.runs);
                        stageData.avgDurationMillisNoPause = Math.floor(stageData.durationMillisNoPause / stageData.runs);
                    }
                    // console.log(stage.durationMillis);
                    // this will give us a number between 1 and 1.5
                    stage.emphasise = (stage.durationMillisNoPause / run.durationMillisNoPause / 2) + 1;
                    stage.emphasise = Math.min(stage.emphasise, 1.5);
                }
            }
        }
    }

    // Run per stage calc's that require data from top level calc's (above)...
    for (var i = 0; i < runGroup.runs.length; i++) {
        var run = runGroup.runs[i];

        if (run.stages && (run.status === 'IN_PROGRESS' || run.status === 'PAUSED_PENDING_INPUT')) {
            addCompletionEstimates(run, runGroup.avgDurationMillisNoPause, runGroup.runs.length);
            for (var ii = 0; ii < run.stages.length; ii++) {
                var stage = run.stages[ii];
                var stageData = getStageData(stage.name, runGroup);

                if (stage.percentCompleteEstimate === undefined) {
                    if (stage.status === 'IN_PROGRESS' || stage.status === 'PAUSED_PENDING_INPUT') {
                        addCompletionEstimates(stage, stageData.avgDurationMillisNoPause, runGroup.runs.length);
                    }
                }
            }
        }
    }
}

function createRunGroups(jobRunsData) {
    jobRunsData.runGroups = [];

    function addRunGroup(runGroupGenerator) {
        addStageTotals(runGroupGenerator.getModelData());
        runGroupGenerator.addPendingStages()
        jobRunsData.runGroups.push(runGroupGenerator.getModelData());
    }

    var curRunGroup = new RunGroupGenerator(maxRunsPerRunGroup);
    for (var i = 0; i < jobRunsData.runs.length; i++) {
        var run = jobRunsData.runs[i];
        if (!curRunGroup.addRun(run)) {
            // current rungroup is full... create a new one...
            addRunGroup(curRunGroup);
            curRunGroup = new RunGroupGenerator();
            curRunGroup.addRun(run);
        }
    }
    addRunGroup(curRunGroup);
    delete jobRunsData.runs;
}

function addEndTimes(jobRunsData) {
    for (var i = 0; i < jobRunsData.runs.length; i++) {
        var run = jobRunsData.runs[i];
        time.setEndTime(run);
        if (run.stages) {
            for (var ii = 0; ii < run.stages.length; ii++) {
		time.setEndTime(run.stages[ii]);
            }
        }
    }
}

/**
 * Add completion estimates to a timedObject.
 * <p/>
 * Basically anything with 'durationMillis', 'avgDurationMillis' etc
 * (i.e. a run, a runGroup or a stage).
 */
function addCompletionEstimates(timedObject, avgDurationMillis, averagedOver) {
    if (averagedOver === 0) {
        // If no runs have completed yet, then we can't make an estimate, so just mark it at 50%.
        timedObject.percentCompleteEstimate = 50;
    } else {
        timedObject.percentCompleteEstimate = (timedObject.durationMillisNoPause / avgDurationMillis * 100);
        // Don't show an estimate of more than 98%, otherwise it will look as though
        // things should be completed when they might not be.
        timedObject.percentCompleteEstimate = Math.min(98, timedObject.percentCompleteEstimate);
        timedObject.percentCompleteEstimate = Math.max(1, timedObject.percentCompleteEstimate);
        timedObject.percentCompleteEstimate = Math.floor(timedObject.percentCompleteEstimate);

        var timeRemainingEstimate = Math.max(0, (avgDurationMillis - timedObject.durationMillisNoPause));
        if (timeRemainingEstimate > 0) {
            timedObject.timeRemainingEstimate = timeRemainingEstimate;
        }
    }
}

function RunGroupGenerator(maxRuns) {
    this.stageData = [];
    this.runs = [];
}
RunGroupGenerator.prototype.addRun = function(run) {
    // Make sure the stage name ordering matches
    for (var i = 0; i < run.stages.length; i++) {
        var stage = run.stages[i];
        if (this.stageData.length > i) {
            if (stage.name !== this.stageData[i].name) {
                // Stages have been changed - reordered or
                // new ones inserted.  Can't add to this group.
                return false;
            }
        } else {
            // Add the stage name to the list for the group
            this.stageData.push({name: stage.name});
        }
    }

    this.runs.push(run);
    return true;
}
RunGroupGenerator.prototype.addPendingStages = function() {
    for (var i = 0; i < this.runs.length; i++) {
        var run  = this.runs[i];
        // fill out the stages in the run if the stages are not all started yet
        if (run.stages.length < this.stageData.length) {
            for (var ii = run.stages.length; ii < this.stageData.length; ii++) {
                run.stages.push({
                    name: this.stageData[ii].name,
                    status: "NOT_EXECUTED",
                    startTimeMillis: 0,
                    endTimeMillis: 0,
                    durationMillis: 0,
                    emphasise: 1
                });
            }
        }
    }
}
RunGroupGenerator.prototype.getModelData = function() {
    if (this.modelData) {
        return this.modelData;
    }
    this.modelData = {};
    this.modelData.stageData = this.stageData;
    this.modelData.runs = this.runs;
    return this.modelData;
}
