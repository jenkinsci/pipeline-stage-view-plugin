/* jslint node: true */
/* global describe, it, expect */

"use strict";

var helper = require('../helper');
var view = helper.require('view/info-action-popover');

describe("view/info-action-popover-spec", function () {

    var pageFrame = '<html><body><p></p></body></html>';

    it("- test_caption_only", function (done) {

        helper.testWithJQuery(pageFrame, function ($) {
            var pageBody = $('body');
            var popoverOn = $('p');

            var alertDom = view.render.call(helper.mvcContext(popoverOn), {
                alert: 'danger',
                caption: 'Failed with the following error(s)'
            }, popoverOn);

            var popoverWidget = helper.require('view/widgets/popover');

            var popover = popoverWidget.newPopover('Info Popover', alertDom, pageBody);
            popover.show();

//            helper.log(pageBody.html());

            expect($('.caption', pageBody).text()).toBe('Failed with the following error(s)');
            expect($('.errors', pageBody).size()).toBe(0);
            expect($('.footer', pageBody).size()).toBe(0);

            done();
        });
    });

    it("- test_caption_and_errors_and_footer", function (done) {

        helper.testWithJQuery(pageFrame, function ($) {
            var pageBody = $('body');
            var popoverOn = $('p');

            var alertDom = view.render.call(helper.mvcContext(popoverOn), {
                alert: 'danger',
                caption: 'Failed with the following error(s)',
                errors: [
                    {
                        label: 'alabel',
                        message: 'amessage'
                    }
                ],
                footer: "toes"
            }, popoverOn);

            var popoverWidget = helper.require('view/widgets/popover');

            var popover = popoverWidget.newPopover('Info Popover', alertDom, pageBody);
            popover.show();

//            helper.log(pageBody.html());

            expect($('.caption', pageBody).text()).toBe('Failed with the following error(s)');
            expect($('.errors .label', pageBody).text()).toBe('alabel'); // the errors
            expect($('.errors .message', pageBody).text()).toBe('amessage'); // the errors
            expect($('.footer', pageBody).text()).toBe('toes');

            done();
        });
    });
});
