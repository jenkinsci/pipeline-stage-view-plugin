/* jslint node: true */
/* global describe, it, expect */

"use strict";

describe("view/pipeline-staged-spec", function () {
    var helper;
    var view;
    var templates;

    var mockApi = jest.genMockFromModule('../../../main/js/model/rest-api');

    beforeEach(() => {
        helper = require('../helper');
        jest.mock('../../../main/js/model/rest-api', () => mockApi)
        view = require('../../../main/js/view/pipeline-staged');
        templates = require('../../../main/js/view/templates');
        // turn off date formatting.
        templates.dateFormatting(false);
    });

    afterEach(() => {
        jest.resetModules();
        jest.resetAllMocks();
    })

    it("- test_render", function (done) {
        helper.testWithJQuery('<div id="frag" objecturl="/jenkins/job/Job%20ABC/" fragCaption="Stage View"></div>', function ($) {
            var modelData = helper.requireTestRes('model/runs_stage_grouped/getModelData/01_expected_modelData');
            var expectedFrag = helper.requireTestRes('view/pipeline_staged/render/expected.html');
            var viewFarg = $('#frag');

            view.render.call(helper.mvcContext(viewFarg), modelData, viewFarg);

            // helper.log('[' + viewFarg.html() + ']');
            if (!helper.compareMultilineText(viewFarg.html(), expectedFrag, true)) {
                expect(viewFarg.html()).toEqual(expectedFrag);
            }

            done();
        });
    });

    var jobUrl = '/job/cd';
    var controllerHtml =
        '<body>' +
        '   <div id="pipeline-staged" cbwf-controller="pipeline-staged" fragCaption="Stage View" objectUrl="' + jobUrl + '" />' +
        '</body>';

    it("- test_render_failed_run", function (done) {
        testStageViewLayout('04_rest_api_runs_failed', done);
    });

    it("- test_render_no_stages_defined", function (done) {
        testStageViewLayout('05_rest_api_runs', done);
    });

    it("- test_render_stages_in_progress", function (done) {
        testStageViewLayout('07_rest_api_runs_in_progress', done);
    });

    function testStageViewLayout(feature, done, devMode) {
        helper.testWithJQuery(controllerHtml, function ($) {
            // Setup mocks
            var runsAPIRes = helper.requireTestRes('model/runs_stage_grouped/getModelData/' + feature);
            mockApi.getJobRuns.mockImplementation((url, callback) => {
                expect(url).toBe(jobUrl);
                // return the mock stage description data...
                callback(runsAPIRes);
            })

            // MVC run...
            helper.mvcRun(['pipeline-staged', 'stage-actions-popover', 'stage-logs', 'feature-tbd',
                           'build-artifacts-popup', 'stage-failed-popover'],
                            $('body'));

            var expectedFrag = helper.requireTestRes('view/pipeline_staged/render/' + feature + '.html');

            var actualHtml = $('body').html();
            if (devMode) {
                helper.log('[' + actualHtml + ']');
            } else {
                if (!helper.compareMultilineText(actualHtml, expectedFrag, true)) {
                    expect(actualHtml).toEqual(expectedFrag);
                }
            }

            done();
        });
    }
});
