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

    it("- test_timezone_usage", function () {
        // Save and set window.timeZone
        const oldTimeZone = global.window && window.timeZone;
        global.window = global.window || {};

        const templates = require('../../../main/js/view/templates');
        templates.dateFormatting(true);

        // 2020-01-01T23:00:00Z in millis
        const utcMillis = Date.UTC(2020, 0, 1, 23, 0, 0, 0);

        const handlebars = require('handlebars');
        const tplTime = handlebars.compile('{{formatDate date "time"}}');
        const tplMonth = handlebars.compile('{{formatDate date "month"}}');
        const tplDom = handlebars.compile('{{formatDate date "dom"}}');
        const tplDate = handlebars.compile('{{formatDate date}}');

        // America/Los_Angeles (UTC-8) => 15:00 Jan 1
        window.timeZone = "America/Los_Angeles";
        let outputTimeLA = tplTime({ date: utcMillis });
        let outputMonthLA = tplMonth({ date: utcMillis });
        let outputDomLA = tplDom({ date: utcMillis });
        let outputDateLA = tplDate({ date: utcMillis });
        expect(outputTimeLA).toMatch(/15:00/);
        expect(outputMonthLA).toMatch(/Jan/);
        expect(outputDomLA).toMatch(/01/);
        expect(outputDateLA).toMatch(/Jan.*01.*15:00/);

        // Asia/Tokyo (UTC+9) => 08:00 Jan 2
        window.timeZone = "Asia/Tokyo";
        let outputTimeTokyo = tplTime({ date: utcMillis });
        let outputMonthTokyo = tplMonth({ date: utcMillis });
        let outputDomTokyo = tplDom({ date: utcMillis });
        let outputDateTokyo = tplDate({ date: utcMillis });
        expect(outputTimeTokyo).toMatch(/08:00/);
        expect(outputMonthTokyo).toMatch(/Jan/);
        expect(outputDomTokyo).toMatch(/02/);
        expect(outputDateTokyo).toMatch(/Jan.*02.*08:00/);

        // Check that the rendered dates and times are different for different timezones
        expect(outputTimeLA).not.toEqual(outputTimeTokyo);
        expect(outputDomLA).not.toEqual(outputDomTokyo);
        expect(outputDateLA).not.toEqual(outputDateTokyo);

        // Restore
        if (oldTimeZone === undefined) {
            delete window.timeZone;
        } else {
            window.timeZone = oldTimeZone;
        }
        templates.dateFormatting(false);
    });

    it("- test_browser_timezone_fallback", function () {
        // Save and clear window.timeZone
        const oldTimeZone = global.window && window.timeZone;
        global.window = global.window || {};
        delete window.timeZone;

        const templates = require('../../../main/js/view/templates');
        templates.dateFormatting(true);

        // 2020-01-01T23:00:00Z in millis
        const utcMillis = Date.UTC(2020, 0, 1, 23, 0, 0, 0);

        // Mock browser timezone to Asia/Tokyo for this test
        const realIntl = global.Intl;
        global.Intl = {
            DateTimeFormat: function() {
                return {
                    resolvedOptions: function() {
                        return { timeZone: "Asia/Tokyo" };
                    }
                }
            }
        };

        const handlebars = require('handlebars');
        const tplTime = handlebars.compile('{{formatDate date "time"}}');
        const tplMonth = handlebars.compile('{{formatDate date "month"}}');
        const tplDom = handlebars.compile('{{formatDate date "dom"}}');
        const tplDate = handlebars.compile('{{formatDate date}}');

        let outputTime = tplTime({ date: utcMillis });
        let outputMonth = tplMonth({ date: utcMillis });
        let outputDom = tplDom({ date: utcMillis });
        let outputDate = tplDate({ date: utcMillis });

        // Should match Asia/Tokyo: 08:00 Jan 2
        expect(outputTime).toMatch(/08:00/);
        expect(outputMonth).toMatch(/Jan/);
        expect(outputDom).toMatch(/02/);
        expect(outputDate).toMatch(/Jan.*02.*08:00/);

        // Restore
        if (oldTimeZone === undefined) {
            delete window.timeZone;
        } else {
            window.timeZone = oldTimeZone;
        }
        templates.dateFormatting(false);
        global.Intl = realIntl;
    });
});
