/* jslint node: true */
/* global describe, it, expect */

"use strict";

var helper = require('../helper');
var view = helper.require('view/run-input-required');

describe("view/run-input-required-spec", function () {

    var pageFrame = '<html><body><p></p></body></html>';
    var inputModelData01 = helper.requireTestRes('view/run_input_required/input-model-01');
    var inputModelData02 = helper.requireTestRes('view/run_input_required/input-model-02');

    function selectPopover($) {
        return $('div[popover-name="run-input-required"]');
    }
    
    it("- test_popover", function (done) {
        
        helper.testWithJQuery(pageFrame, function ($) {
            var popoverOn = $('p');

            view.render(inputModelData01, popoverOn);
            
            var inputPopover = selectPopover($);

            // Should not be visible atm ... need to mouseover.
            expect(inputPopover.length).toBe(0);

            // Move the mouse over the anchor element and make the popover visible.
            popoverOn.mouseenter();

            inputPopover = selectPopover($);
            
            // Should be visible now.
            expect(inputPopover.length).toBe(1);

            // Check the caption
            expect($('.caption', inputPopover).text()).toBe('Workflow Configuration');            

            // Check the inputs
            var inputs = $('.inputs :input', inputPopover);
            expect(inputs.length).toBe(5);
            var index = 0;
            expect($(inputs.get(index++)).is(':checkbox')).toBe(true);
            expect($(inputs.get(index++)).is(':checkbox')).toBe(true);
            expect($(inputs.get(index++)).is(':text')).toBe(true);
            expect($(inputs.get(index++)).is(':password')).toBe(true);
            expect($(inputs.get(index++)).is('select')).toBe(true);

            // Check the buttons
            var buttons = $('.buttons button', inputPopover);
            expect(buttons.length).toBe(2);
            expect($(buttons.get(0)).text()).toBe('Okay');            
            expect($(buttons.get(1)).text()).toBe('Abort');
            
            done();
        });
    });

    it("- test_abort", function (done) {
        
        helper.testWithJQuery(pageFrame, function ($) {
            var popoverOn = $('p');

            view.render(inputModelData01, popoverOn);

            // Move the mouse over the anchor element and make the popover visible.
            popoverOn.mouseenter();
            // Check that the popover has appeared
            expect(selectPopover($).length).toBe(1);
            
            // Mock the ajax call and test it
            var ajax = helper.require('util/ajax');
            ajax.jenkinsAjaxPOST = function(abortUrl, callback) {
                // check the Url used
                expect(abortUrl).toBe('/jenkins/job/Workflow/24/input/Run-test-suites/abort');
                
                callback();
                
                // Check that the popover has disappeared
                expect(selectPopover($).length).toBe(0);
                
                done();
            }
            
            // Click the abort button
            $('.buttons button.abort-button').click();
        });
    });

    it("- test_proceed_ok", function (done) {
        
        helper.testWithJQuery(pageFrame, function ($) {
            var popoverOn = $('p');

            view.render(inputModelData01, popoverOn);

            // Move the mouse over the anchor element and make the popover visible.
            popoverOn.mouseenter();
            // Check that the popover has appeared
            expect(selectPopover($).length).toBe(1);
            
            // Mock the ajax call and test it
            var ajax = helper.require('util/ajax');
            ajax.jenkinsAjaxPOSTURLEncoded = function(proceedUrl, parameters, callback) {
                // check the Url used
                expect(proceedUrl).toBe('/jenkins/job/Workflow/24/wfapi/inputSubmit?inputId=Run-test-suites');
                
                var inputNVPs = JSON.parse(parameters.json);
                // console.log(inputNVPs);
                var index = 0;
                expect(inputNVPs.parameter[index].name).toBe('Run test suites?');
                expect(inputNVPs.parameter[index++].value).toBe(true);
                expect(inputNVPs.parameter[index].name).toBe('Good boy?');
                expect(inputNVPs.parameter[index++].value).toBe(false);
                expect(inputNVPs.parameter[index].name).toBe('Enter some text');
                expect(inputNVPs.parameter[index++].value).toBe('Hello');
                expect(inputNVPs.parameter[index].name).toBe('Enter a passord');
                expect(inputNVPs.parameter[index++].value).toBe('MyPasswd');
                expect(inputNVPs.parameter[index].name).toBe('Take your pick');
                expect(inputNVPs.parameter[index++].value).toBe('Choice 1');
                
                callback();
                
                // Check that the popover has disappeared
                expect(selectPopover($).length).toBe(0);
                
                done();
            }
            
            // Click the proceed button
            $('.buttons button.proceed-button').click();
        });
    });
    
    it("- test_proceed_unsupported_input_redirect", function (done) {
        
        helper.testWithJQuery(pageFrame, function ($) {
            var popoverOn = $('p');

            view.render(inputModelData02, popoverOn);

            // Move the mouse over the anchor element and make the popover visible.
            popoverOn.mouseenter();
            // Check that the popover has appeared
            var popover = selectPopover($);
            expect(popover.length).toBe(1);
            
            // Check that the advanced inputs redirect dialog has appeared
            expect($('.has-advanced-inputs', popover).length).toBe(1);            
            
            // Check the redirect URL
            expect($('.has-advanced-inputs a', popover).attr('href')).toBe(inputModelData02.redirectApprovalUrl);            
            
            done();
        });
    });    
    
});
