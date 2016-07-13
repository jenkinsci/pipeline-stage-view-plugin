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

var jqProxy = require('../jQuery');
var charts = require('./widgets/charts');
var dialogWidget = require('./widgets/dialog');
var templates = require('./templates');
var extpAPI = require('jenkins-js-extp/API');
var ExtensionPointContainer = extpAPI.ExtensionPointContainer;
var ExtensionPoint = extpAPI.ExtensionPoint;

var extensionPointContainer = new ExtensionPointContainer();

exports.extensionPointContainer = extensionPointContainer;

exports.render = function (jobRunsData, onElement) {
    var mvcContext = this;
    var fragCaption = mvcContext.attr('fragCaption');

    _render(jobRunsData, onElement, fragCaption);

    // listen for changes to the model...
    this.onModelChange(function(event) {
        _render(event.modelData, onElement, fragCaption);
    });
};

function _render(jobRunsData, onElement, fragCaption) {

    // If looking for an example of the model consumed by this view, see the "*_expected_*.json"
    // files in ui/src/test/resources/model/runs_stage_grouped/getModelData.
    // Of course, you can also just use console.log(jobRunsData).
    // The expected model is that produced by ui/src/main/js/model/runs-stage-grouped.js

    if (jobRunsData.runGroups && jobRunsData.runGroups.length > 0) {
        var $ = jqProxy.getJQuery();
        var runGroup = jobRunsData.runGroups[0];

        runGroup.fragCaption = fragCaption;
        runGroup.maxTableEms = runGroup.stageData.length * 10;

        var pipelineStagedDom = templates.apply('pipeline-staged', runGroup);
        addLaneCharts(pipelineStagedDom, runGroup);
        var viewPort = $('div.table-viewPort');
        if (viewPort && viewPort.size() > 0) { // First rendering does not have an existing element
            var leftScroll = viewPort[0].scrollLeft;  // This way we can jump back to the previous scroll position
            onElement.empty().append(pipelineStagedDom); // With many stages, the DOM change scrolls us back to start if we don't reset scroll.
            viewPort = $('div.table-viewPort')[0]; // Re-fetched because the DOM has changed in above.
            if (leftScroll < viewPort.scrollWidth) {
                viewPort.scrollLeft = leftScroll;
            }
        } else {
            onElement.empty().append(pipelineStagedDom);
        }

        addExtensionPoints(onElement, runGroup);
    }
}

function addLaneCharts(jQueryDom, runGroup) {
    var $ = jqProxy.getJQuery();
    var stageData = runGroup.stageData;
    var avgStageTimes = [];

    for (var i = 0; i < stageData.length; i++) {
        avgStageTimes.push(stageData[i].avgDurationMillisNoPause);
    }

    var stackedBarChartAnchorEls = $('.totals .stackedBarChart', jQueryDom);

    // Should be the same number of these as there are array elements in stageData
    if (stackedBarChartAnchorEls.size() !== avgStageTimes.length) {
        console.log('Unexpected problem.  Template failed to generate a lane header for all stages. ' + stackedBarChartAnchorEls.size() + ' != ' + avgStageTimes.length);
        return;
    }

    for (var i = 0; i < stackedBarChartAnchorEls.size(); i++) {
        var stackedBarChartAnchorEl = $(stackedBarChartAnchorEls.get(i));
        var stackedBarChart = charts.stackedBarChart(avgStageTimes, i);

        stackedBarChartAnchorEl.append(stackedBarChart);
    }
}

function addExtensionPoints(onElement, runGroupData) {
    var $ = jqProxy.getJQuery();

    // We need to clear and recreate every time here because of how the stage
    // is in a continuous re-render mode (polling). Having push notifications
    // will clean up a lot of this.

    // Clear the last set of ExtensionPoint instances.
    extensionPointContainer.remove();

    function addExtensionPoint(extensionPoint) {
        extensionPointContainer.add(extensionPoint);
        extensionPoint.oncontribute(function() {
            var theContribution = this;

            if (theContribution.activatorContentType !== 'className') {
                throw 'Jenkins Pipeline Stage View only support CSS className activation widget types.';
            }

            // Add the activator to the dock.
            var activatorDock = $(extensionPoint.dock);
            var activatorWidget = $('<span class="extension">');
            activatorWidget.addClass(theContribution.activatorContent);
            activatorWidget.attr('title', theContribution.caption);
            activatorDock.append(activatorWidget);
            activatorWidget.click(function() {
                // Display the extension content in a dialog.
                var content = $('<div>');
                content.append(theContribution.content);
                var dialog = dialogWidget.show(theContribution.caption, content);
                theContribution.onHide(function() {
                    dialog.hide();
                });
                theContribution.show(content.get());
            });
        });
    }

    // Find all runs.
    var runs = $('.job', onElement);
    runs.each(function() {
        var run = $(this);
        var runId = run.attr('data-runId');

        if (!runId) {
            console.warn('No "data-runId" on run.');
            return;
        }

        var runObj = getRun(runId, runGroupData);

        // Run level extension points...
        var stageEndIcons = $('.stage-start .stage-end-icons', run);
        var extensionPoint = new ExtensionPoint('jenkins.pipeline.run', runId, stageEndIcons.get());
        addExtensionPoint(extensionPoint);

        // Tack on the REST API links associated with the run.
        // Putting them in private for now so as discourage external use.
        extensionPoint._private._links = runObj._links;

        // Add ExtensionPoint instances for each stage in the run.
        var stages = $('.stage-cell', run);
        stages.each(function() {
            var stage = $(this);
            var stageId = stage.attr('data-stageId');

            if (!stageId) {
                console.warn('No "data-stageId" on stage.');
                return;
            }

            var stageObj = getStage(runId, stageId, runGroupData);
            var extensionPointID = runId + ':' + stageId;
            var extensionDock = $('.extension-dock', stage);
            var extensionPoint = new ExtensionPoint('jenkins.pipeline.run.stage', extensionPointID, extensionDock.get());

            // When other plugins contribute to the stage view, we add a little activation
            // widget to the stage in the ExtensionPoint dock area.
            addExtensionPoint(extensionPoint);

            // Tack on the REST API links associated with the stage.
            // Putting them in private for now so as discourage external use.
            extensionPoint._private._links = stageObj._links;
        });
    });

    // We've changed the set of ExtensionPoints, so notify anyone
    // that's interested.
    extensionPointContainer.notifyOnChange();
}

function getRun(runId, runGroupData) {
    for (var i = 0; i < runGroupData.runs.length; i++) {
        var run = runGroupData.runs[i];
        if (run.id === runId) {
            return run;
        }
    }
    return undefined;
}

function getStage(runId, stageId, runGroupData) {
    var run = getRun(runId, runGroupData);
    if (run) {
        for (var i = 0; i < run.stages.length; i++) {
            var stage = run.stages[i];
            if (stage.id === stageId) {
                return stage;
            }
        }
    }
    return undefined;
}
