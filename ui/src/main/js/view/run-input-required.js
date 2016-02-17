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
var popoverWidget = require('./widgets/popover');
var ajax = require('../util/ajax');

var supportedInputTypes = {
    'BooleanParameterDefinition': true,
    'StringParameterDefinition': true,
    'TextParameterDefinition': true,
    'PasswordParameterDefinition': true,
    'ChoiceParameterDefinition': true
};

function allInputTypesSupported(inputs) {
    if (!inputs) {
        // no inputs.
        return true;
    }
    for (var i = 0; i < inputs.length; i++) {
        if (!supportedInputTypes[inputs[i].type]) {
            return false;
        }
    }
    return true;
}

exports.render = function (inputRequiredModel, onElement) {
    if (allInputTypesSupported(inputRequiredModel.inputs)) {
        var popoverDom = templates.apply('run-input-required', inputRequiredModel);

        var popover = popoverWidget.newPopover('run-input-required', popoverDom, onElement.parent(), {
            hoverBoth: true,
            namespace: 'runInputRequired',
            placement: 'left',
            onshow: function() {
                var $ = jqProxy.getJQuery();
                var pendingActionsTable = $('.pending-input-actions', popoverDom);

                var abortUrl = inputRequiredModel.abortUrl;
                var proceedUrl = inputRequiredModel.proceedUrl;
                var proceedBtn = $('.proceed-button', popoverDom);
                var abortBtn = $('.abort-button', popoverDom);

                function disableButtons() {
                    proceedBtn.attr("disabled", true);
                    abortBtn.attr("disabled", true);
                }

                proceedBtn.click(function() {
                    disableButtons();

                    // Get all the inputs and POST them back tot he Run API.
                    var inputs = $('.inputs :input');
                    var inputNVPs = [];
                    inputs.each(function() {
                        var input = $(this);
                        var inputName = input.attr('name');

                        if (input.is(':checkbox')) {
                            // Convert the checkbox "on"/"off" value to a boolean.
                            inputNVPs.push({
                                name: inputName,
                                value: input.is(':checked')
                            });
                        } else {
                            inputNVPs.push({
                                name: inputName,
                                value: input.val()
                            });
                        }
                    });

                    // Perform the POST. Needs to be encoded into a "json" parameter with an
                    // array object named "parameter" :)
                    var parameters = {
                        json: Object.toJSON({
                            parameter: inputNVPs
                        })
                    };
                    ajax.jenkinsAjaxPOSTURLEncoded(proceedUrl, parameters, function() {
                        popover.hide();
                    });
                });
                abortBtn.click(function() {
                    disableButtons();
                    ajax.jenkinsAjaxPOST(abortUrl, function() {
                        popover.hide();
                    });
                });
            }
        });
        popover.hover();

        // for testing...
        return popoverDom;
    } else {
        var popoverDom = templates.apply('run-input-required-redirect', inputRequiredModel);

        var popover = popoverWidget.newPopover('run-input-required', popoverDom, onElement.parent(), {
            hoverBoth: true,
            namespace: 'runInputRequired',
            placement: 'left'});

        popover.hover();

        // for testing...
        return popoverDom;
    }
};
