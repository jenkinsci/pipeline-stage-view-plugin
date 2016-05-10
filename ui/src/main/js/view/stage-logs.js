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
var templates = require('./templates');
var dialog = require('./widgets/dialog');

exports.render = function (stageDescription, onElement) {
    var $ = jqProxy.getJQuery();
    var theWindow = require('window-handle').getWindow();
    var stageLogsDom = templates.apply('stage-logs', stageDescription);
    var winWidth = $(theWindow).width();
    var winHeight = $(theWindow).height();
    var dialogWidth = Math.min(800, (winWidth * 0.7));
    var dialogHeight = Math.min(800, (winHeight * 0.7));
    var nodeLogFrames = $('.node-log-frame', stageLogsDom);
    var nodeNameBars = $('.node-name', stageLogsDom);

    stageLogsDom.addClass('stageLogsPopover');

    var clickNSEvent = 'click.cbwf-stage-logs';

    onElement.off(clickNSEvent);
    onElement.on(clickNSEvent, function() {
        dialog.show('Stage Logs (' + stageDescription.name + ')', stageLogsDom, {
            classes: 'cbwf-stage-logs-dialog',
            placement: 'window-visible-top',
            onElement: onElement,
            width: dialogWidth,
            height: dialogHeight,
            onshow: function() {
                var header = $('.cbwf-stage-logs-dialog .header');

                nodeNameBars.click(function() {
                    var nodeNameBar = $(this);
                    var nodeLogFrame = nodeNameBar.parent();
                    var active = nodeLogFrame.hasClass('active');

                    // add active state to the clicked log frame if was previously inactive
                    if (!active) {
                        // Hide any other log box
                        nodeLogFrames.removeClass('active');
                        // Show this one
                        nodeLogFrame.addClass('active');
                    }
                });

                if (nodeNameBars.size() === 1) {
                    nodeNameBars.click();
                }
            }
        });
    });

    // for testing...
    return stageLogsDom;
}
