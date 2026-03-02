/* jslint node: true */
/* global describe, it, expect */

"use strict";

describe("model/runs-stage-grouped-spec", function () {
    var helper;
    var model;

    var mockAjax = jest.genMockFromModule('../../../main/js/util/ajax');

    beforeEach(() => {
        helper = require('../helper');
        jest.mock('../../../main/js/util/ajax', () => mockAjax);
        model = require('../../../main/js/model/runs-stage-grouped')
    })

    afterEach(() => {
        jest.resetModules();
        jest.resetAllMocks();
    })

    it("- test_01_getModelData", function (done) {
        helper.testWithJQuery('<div objectUrl="/jenkins/job/xxxJob"></div>', function ($) {
            var targetEl = $('div');

            mockAjax.execAsyncGET.mockImplementation((resPathTokens, callback) => {
                expect(resPathTokens.length).toEqual(3);
                expect(resPathTokens[0]).toEqual('/jenkins/job/xxxJob');
                expect(resPathTokens[1]).toEqual('wfapi');
                expect(resPathTokens[2]).toEqual('runs');

                var restApiJobHistoryData = helper.requireTestRes('model/runs_stage_grouped/getModelData/01_rest_api_jobHistory');
                callback(restApiJobHistoryData);
            });

            model.setMaxRunsPerRunGroup(5);
            model.getModelData.call(
                helper.mvcContext(targetEl),
                function (modelData) {
                    var expectedModel = helper.requireTestRes('model/runs_stage_grouped/getModelData/01_expected_modelData');
                    //helper.log(modelData);
                    expect(modelData).toEqual(expectedModel);

                    done();
                });
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
        test_runGroupGeneration('03_rest_api_runs', '03_expected_modelData_fixed', done);
    });

    it("- test_05_getModelData_stages_undefined", function (done) {
        // no stage info but the ran successfully. should have an empty stage[] on the run.
        test_runGroupGeneration('05_rest_api_runs', '05_expected_modelData', done);
    });

    it("- test_09_getModelData_failed_runs_excluded_from_stage_times", function (done) {
        // Test that stage times are only calculated from successful runs
        helper.testWithJQuery('<div objectUrl="/jenkins/job/xxxJob"></div>', function ($) {
            var targetEl = $('div');

            mockAjax.execAsyncGET.mockImplementation((resPathTokens, callback) => {
                var restApiJobHistoryData = helper.requireTestRes('model/runs_stage_grouped/getModelData/09_rest_api_mixed_statuses');
                callback(restApiJobHistoryData);
            });

            model.getModelData.call(
                helper.mvcContext(targetEl),
                function (modelData) {
                    // We have 3 runs: #1 SUCCESS, #2 FAILED, #3 SUCCESS
                    // Build stage: 2s (run 1) + 3s (run 3) = 5s total / 2 runs = 2.5s avg
                    // Test stage: 5s (run 1) + 5s (run 3) = 10s total / 2 runs = 5s avg
                    // The failed run (#2) should not be included in calculations
                    
                    var runGroup = modelData.runGroups[0];
                    var buildStageData = runGroup.stageData[0];
                    var testStageData = runGroup.stageData[1];
                    
                    expect(buildStageData.name).toEqual("Build");
                    expect(buildStageData.runs).toEqual(2); // Only successful runs
                    expect(buildStageData.durationMillis).toEqual(5000); // 2000 + 3000
                    expect(buildStageData.avgDurationMillis).toEqual(2500); // 5000 / 2
                    
                    expect(testStageData.name).toEqual("Test");
                    expect(testStageData.runs).toEqual(2); // Only successful runs
                    expect(testStageData.durationMillis).toEqual(10000); // 5000 + 5000
                    expect(testStageData.avgDurationMillis).toEqual(5000); // 10000 / 2
                    
                    done();
                });
        });
    });

    function test_runGroupGeneration(restApiResponseFile, expectedModelFile, done, devMode) {
        helper.testWithJQuery('<div objectUrl="/jenkins/job/xxxJob"></div>', function ($) {
            var targetEl = $('div');

            mockAjax.execAsyncGET.mockImplementation((resPathTokens, callback) => {
                var restApiJobHistoryData = helper.requireTestRes('model/runs_stage_grouped/getModelData/' + restApiResponseFile);
                callback(restApiJobHistoryData);
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

                done();
            });
        });
    }
});
