/* jslint node: true */
/* global describe, it, expect */

"use strict";

var helper = require('../helper');
var model = helper.require('model/runs-stage-grouped');

describe("model/runs-stage-grouped-spec", function () {

    it("- test_01_getModelData", function (done) {
        helper.testWithJQuery('<div objectUrl="/jenkins/job/xxxJob"></div>', function ($) {
            var targetEl = $('div');

            helper.mock('util/ajax', {
                execAsyncGET: function (resPathTokens, callback) {
                    expect(resPathTokens.length).toEqual(3);
                    expect(resPathTokens[0]).toEqual('/jenkins/job/xxxJob');
                    expect(resPathTokens[1]).toEqual('wfapi');
                    expect(resPathTokens[2]).toEqual('runs');

                    var restApiJobHistoryData = helper.requireTestRes('model/runs_stage_grouped/getModelData/01_rest_api_jobHistory');
                    callback(restApiJobHistoryData);
                }
            });

            model.setMaxRunsPerRunGroup(5);
            model.getModelData.call(
                helper.mvcContext(targetEl),
                function (modelData) {
                    var expectedModel = helper.requireTestRes('model/runs_stage_grouped/getModelData/01_expected_modelData');
                    //helper.log(modelData);
                    expect(modelData).toEqual(expectedModel);
                });

            done();
        });
    });

    it("- test_01_getModelData_small_groups", function (done) {
        model.setMaxRunsPerRunGroup(2);
        // 01_expected_modelData_small_groups should have 2 runGroups, with 2 runs in the
        // first and 1 run in the second
        test_runGroupGeneration('01_rest_api_jobHistory', '01_expected_modelData_small_groups', done);
    });

    it("- test_02_getModelData_reordered_stages", function (done) {
        model.setMaxRunsPerRunGroup(5);
        // 02_expected_modelData should have 2 runGroups, with 1 run in the
        // first and 2 run in the second because there was a stage rename in the second run
        test_runGroupGeneration('02_rest_api_jobHistory', '02_expected_modelData', done);
    });

    it("- test_03_getModelData_in_progress", function (done) {
        model.setMaxRunsPerRunGroup(2);
        // 02_expected_modelData should have 2 runGroups, with 1 run in the
        // first and 2 run in the second because there was a stage rename in the second run
        test_runGroupGeneration('03_rest_api_runs', '03_expected_modelData', done);
    });

    it("- test_05_getModelData_stages_undefined", function (done) {
        // no stage info but the ran successfully. should have an empty stage[] on the run.
        test_runGroupGeneration('05_rest_api_runs', '05_expected_modelData', done);
    });

    function test_runGroupGeneration(restApiResponseFile, expectedModelFile, done, devMode) {
        helper.testWithJQuery('<div objectUrl="/jenkins/job/xxxJob"></div>', function ($) {
            var targetEl = $('div');

            helper.mock('util/ajax', {
                execAsyncGET: function (resPathTokens, callback) {
                    var restApiJobHistoryData = helper.requireTestRes('model/runs_stage_grouped/getModelData/' + restApiResponseFile);
                    callback(restApiJobHistoryData);
                }
            });

            model.getModelData.call(
                helper.mvcContext(targetEl),
                function (modelData) {
                if (devMode) {
                    helper.log(modelData);
                } else {
                    var expectedModel = helper.requireTestRes('model/runs_stage_grouped/getModelData/' + expectedModelFile);
                    expect(modelData).toEqual(expectedModel);
                }
            });

            done();
        });
    }
});
