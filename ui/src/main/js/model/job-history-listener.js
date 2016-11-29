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

var restApi = require('./rest-api');

exports.listen = function (jobUrl, callback) {
    restApi.getJobRuns(jobUrl, function (jobRunsData) {
        callback(jobRunsData);
        setupJobPoll(jobUrl, callback, jobRunsData)
    }, {fullStages: 'true'});
}

exports.schedulePoll = function (callback) {
    // Tests can mock this function for testing.
    setTimeout(callback, 5000);
}

function setupJobPoll(jobUrl, callback, jobRunsData) {
    function findSinceRunParam() {
        // console.log('findSinceRunParam');
        // Find the name of the oldest build that has an in progress type status.
        // If there's none that fit that, use the latest build.
        var i = jobRunsData.length - 1;
        for (; i >= 0; i--) {
            var runStatus = jobRunsData[i].status;
            if (runStatus === 'IN_PROGRESS' || runStatus === 'PAUSED_PENDING_INPUT') {
                return jobRunsData[i].name;
            }
        }
        if (jobRunsData.length > 0) {
            return jobRunsData[0].name;
        } else {
            return undefined;
        }
    }

    function findRunIndex(id) {
        for (var i = 0; i < jobRunsData.length; i++) {
            if (jobRunsData[i].id === id) {
                return i;
            }
        }
        return -1;
    }

    function addRun(run) {
        jobRunsData = [run].concat(jobRunsData);
    }

    function pollJobRuns () {
        restApi.getJobRuns(jobUrl, function (sinceJobRunsData) {
        // console.log('job-progress......');
            try {
                var notifyListeners = false;

                // reverse iterate the returned set and see if there's anything new
                // or potentially changed...
                for (var i = sinceJobRunsData.length - 1; i >= 0; i--) {
                    var aSinceRun = sinceJobRunsData[i];
                    var knownRunIndex = findRunIndex(aSinceRun.id);

                    if (knownRunIndex === -1) {
                        // We don't know this run... it's a new one.  Add it to the start.
                        notifyListeners = true;
                        addRun(aSinceRun);
                    } else {
                        // We know this run... has it changed?
                        var knownRun = jobRunsData[knownRunIndex];
                        if (aSinceRun.status !== knownRun.status) {
                            // status has changed
                            notifyListeners = true;
                        } else if (aSinceRun.status === 'IN_PROGRESS' || aSinceRun.status === 'PAUSED_PENDING_INPUT') {
                            // it's in an in progress state of some sort...
                            notifyListeners = true;
                        }
                        jobRunsData[knownRunIndex] = aSinceRun;
                    }
                }

                // TODO: what about jobs that have been deleted?

                if (notifyListeners) {
                    callback(jobRunsData);
                }
            } finally {
                exports.schedulePoll(pollJobRuns);
            }
        }, {since: findSinceRunParam(), fullStages: 'true'});
    }

    // Kick it ...
    exports.schedulePoll(pollJobRuns);
}
