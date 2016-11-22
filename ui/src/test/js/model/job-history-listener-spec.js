/* jslint node: true */
/* global describe, it, expect */

"use strict";

var helper = require('../helper');
var listener = helper.require('model/job-history-listener');

describe("model/job-history-listener", function () {

    it("- test_01", function () {

        //var _01_run_changeset_pre_model = helper.requireTestRes('model/run_changesets/01_run_changeset_pre_model');
        var jobsToReturnFromRestAPI = [];
        var pollJobRunsFunc;
        var lastParams;
        var listenCallbackCallCount = 0;

        function addRun(name, id, status) {
            jobsToReturnFromRestAPI = [{name: name, id: id, status: status}].concat(jobsToReturnFromRestAPI);
        }

        helper.mock('model/rest-api', {
            getJobRuns: function (jobUrl, callback, params) {
                // create a completely different obj graph by serializing and deserializing...
                var asString = JSON.stringify(jobsToReturnFromRestAPI);
                var backToObj = JSON.parse(asString);
                callback(backToObj);
                lastParams = params;
            }
        });
        helper.mock('model/job-history-listener', {
            schedulePoll: function (pollJobRuns) {
                // mocking this so as to disable the timer, allowing us to control it
                pollJobRunsFunc = pollJobRuns;
            }
        });

        expect(pollJobRunsFunc === undefined).toEqual(true);
        expect(listenCallbackCallCount).toEqual(0);
        listener.listen('/job/AAA', function (jobModel) {
            // helper.log(jobModel);
            expect(jobModel).toEqual(jobsToReturnFromRestAPI);
            listenCallbackCallCount++;
        });

        // schedulePoll should get called => pollJobRunsFunc should get initialized
        expect(pollJobRunsFunc !== undefined).toEqual(true);
        // listenCallbackCallCount should get called with the initial data load...
        expect(listenCallbackCallCount).toEqual(1);

        // Calling the pollJobRunsFunc should have no effect on listenCallbackCallCount
        // because the model has not changed...
        pollJobRunsFunc();
        pollJobRunsFunc();
        pollJobRunsFunc();
        expect(listenCallbackCallCount).toEqual(1);

        // Add a run to the job and then call pollJobRunsFunc again... should trigger the
        // the listen callback and inc listenCallbackCallCount.
        addRun('#1', 1, 'SUCCESS');
        pollJobRunsFunc();
        expect(listenCallbackCallCount).toEqual(2);

        // Calling pollJobRunsFunc should have no effect until something changes...
        pollJobRunsFunc();
        pollJobRunsFunc();
        expect(listenCallbackCallCount).toEqual(2);

        // Now add an IN_PROGRESS run.  This should behave differently.  We always treat
        // IN_PROGRESS as being "dirty", so calling pollJobRunsFunc a few times after should
        // inc listenCallbackCallCount
        addRun('#2', 2, 'IN_PROGRESS');
        pollJobRunsFunc();
        expect(listenCallbackCallCount).toEqual(3);
        pollJobRunsFunc();
        pollJobRunsFunc();
        expect(listenCallbackCallCount).toEqual(5);

        // Now lets change the status of that build and check how pollJobRunsFunc runs
        // the next time. listenCallbackCallCount should just get inc'd once.
        jobsToReturnFromRestAPI[0].status = 'SUCCESS';
        pollJobRunsFunc();
        expect(listenCallbackCallCount).toEqual(6);
        pollJobRunsFunc();
        pollJobRunsFunc();
        expect(listenCallbackCallCount).toEqual(6);
    });

});
