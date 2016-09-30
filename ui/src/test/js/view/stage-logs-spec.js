/* jslint node: true */
/* global describe, it, expect */

"use strict";

var helper = require('../helper');

describe("view/stage-logs", function () {

    // TODO: maybe rename src/test/resources/model/runs_stage_grouped/checkpoint_resumed_stage_description.json to something that doesn't include the word "checkpoint"? Not a biggie.
    var sampleStageDesc = helper.requireTestRes('model/runs_stage_grouped/checkpoint_resumed_stage_description');
    var stageDescEndpoint = '/jenkins/job/Wokflow%20XX/7/execution/node/9/wfapi/describe'; // see the 'describe' link in sampleStageDesc
    var controllerHtml =
        '<body>' +
        '   <div id="stage-actions-popover" cbwf-controller="stage-actions-popover" descUrl="' + stageDescEndpoint + '" />' +
        '</body>';

    it("- test_01", function (done) {

        helper.testWithJQuery(controllerHtml, function ($) {
            // Setup mocks
            helper.mock('model/rest-api', {
                getDescription: function (url, callback) {
                    expect(url).toBe(stageDescEndpoint);
                    // return the mock stage description data...
                    callback(sampleStageDesc);
                },
                getObject: function (url, callback) {
                    expect(url).toBe('/jenkins/job/Wokflow%20XX/7/execution/node/12/wfapi/log');
                    // return the mock stage description data...
                    callback({
                        "nodeId": "12",
                        "nodeStatus": "SUCCESS",
                        "length": 6,
                        "hasMore": false,
                        "text": "build",
                        "consoleUrl": "/jenkins/job/Wokflow%20XX/7/execution/node/12/log"
                    });
                }
            });

            // MVC run...
            helper.mvcRun(['stage-actions-popover', 'stage-logs', 'feature-tbd', 'node-log'], $('body'));

            // Trigger the menu widget to appear...
            helper.log("expect($('#stage-actions-popover').is(':visible')).toBe(true); ...and is... " + $('#stage-actions-popover').is(':visible'));
            expect($('#stage-actions-popover').is(':visible')).toBe(true);
            $('#stage-actions-popover').mouseenter();

            // DISABLED: Trigger the menu items popup...
            // Pop menu is replaced with pop-alert...
            $('.stage-actions-popover').mouseenter();
            helper.log("expect($('.cbwf-popout').is(':visible')).toBe(true); ...and is... " + $('.cbwf-popover').is(':visible'));
            expect($('.cbwf-popover').is(':visible')).toBe(true);


            // Click the stage logs menu item and make the dialog appear
            helper.log("expect($(\"[cbwf-controller='stage-logs']\").is(':visible')).toBe(true); ...and is... " + $('[cbwf-controller=\'stage-logs\']').is(':visible'));
            expect($("[cbwf-controller='stage-logs']").is(':visible')).toBe(true);

            $("[cbwf-controller='stage-logs']").click();
            helper.log("expect($(\"[cbwf-controller='stage-logs']\").is(':visible')).toBe(true); ...and is... " + $('[cbwf-controller=\'stage-logs\']').is(':visible'));
            expect($('.cbwf-stage-logs-dialog').is(':visible')).toBe(true);

            var nodeLogFrames = $('.node-log-frame');

            // TODO: hmmmm ... we may need to modify the test data model here ... see the comment below.
            // There should only be 1 log frame per step (with ones not having logs omitting the step)
            expect(nodeLogFrames.size()).toBe(3);

            // Click on the first log
            var firstLogNode = $(nodeLogFrames.get(0));
            firstLogNode.click();

            // logDetails of that log should be filled in...
            helper.log(firstLogNode.html());
            expect($('.log-details .console-output', firstLogNode).text()).toBe('build');
            expect($('.log-details .console-output', firstLogNode).is(':visible')).toBe(true);

            // console.log($('body').html());

            done();
        });

    });

});
