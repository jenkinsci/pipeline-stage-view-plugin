/* jslint node: true */
/* global describe, it, expect */

"use strict";

describe("model/runs-stage-grouped-debug", function () {
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

    it("- test_full_run_time_calculation", function (done) {
        helper.testWithJQuery('<div objectUrl="/jenkins/job/xxxJob"></div>', function ($) {
            var targetEl = $('div');

            // Create test data similar to the screenshot
            var testData = [
                {
                    "name": "#9",
                    "status": "SUCCESS", 
                    "durationMillis": 10000, // 10s
                    "pauseDurationMillis": 0,
                    "stages": [
                        {"name": "Build", "status": "SUCCESS", "durationMillis": 2000, "pauseDurationMillis": 0},
                        {"name": "Hello", "status": "SUCCESS", "durationMillis": 3000, "pauseDurationMillis": 0},
                        {"name": "Test", "status": "SUCCESS", "durationMillis": 5000, "pauseDurationMillis": 0}
                    ]
                },
                {
                    "name": "#8",
                    "status": "FAILED",
                    "durationMillis": 25000, // 25s
                    "pauseDurationMillis": 0,
                    "stages": [
                        {"name": "Build", "status": "SUCCESS", "durationMillis": 2000, "pauseDurationMillis": 0},
                        {"name": "Hello", "status": "SUCCESS", "durationMillis": 3000, "pauseDurationMillis": 0},
                        {"name": "Test", "status": "FAILED", "durationMillis": 20000, "pauseDurationMillis": 0}
                    ]
                },
                {
                    "name": "#7",
                    "status": "FAILED",
                    "durationMillis": 25000,
                    "pauseDurationMillis": 0,
                    "stages": [
                        {"name": "Build", "status": "SUCCESS", "durationMillis": 2000, "pauseDurationMillis": 0},
                        {"name": "Hello", "status": "SUCCESS", "durationMillis": 3000, "pauseDurationMillis": 0},
                        {"name": "Test", "status": "FAILED", "durationMillis": 20000, "pauseDurationMillis": 0}
                    ]
                }
            ];

            mockAjax.execAsyncGET.mockImplementation((resPathTokens, callback) => {
                callback(testData);
            });

            model.setMaxRunsPerRunGroup(10); // Ensure all runs are in one group
            model.getModelData.call(
                helper.mvcContext(targetEl),
                function (modelData) {
                    console.log("Top-level avgDurationMillisNoPause:", modelData.avgDurationMillisNoPause);
                    console.log("RunGroup[0] avgDurationMillisNoPause:", modelData.runGroups[0].avgDurationMillisNoPause);
                    
                    // The full run time should be ~10s (only from the successful run)
                    expect(modelData.runGroups[0].avgDurationMillisNoPause).toEqual(10000);
                    
                    done();
                });
        });
    });
});