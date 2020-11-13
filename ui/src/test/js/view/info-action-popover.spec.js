/* jslint node: true */
/* global describe, it, expect */

"use strict";

describe("view/info-action-popover-spec", function () {
    var helper;
    var view;

    var pageFrame = '<html><body><p></p></body></html>';

    beforeEach(() => {
        helper = require('../helper');
        view = require('../../../main/js/view/info-action-popover');
    })

    afterEach(() => {
        jest.resetModules();
        jest.resetAllMocks();
    })

    it("- test_caption_only", function (done) {

        helper.testWithJQuery(pageFrame, function ($) {
            var pageBody = $('body');
            var popoverOn = $('p');

            var alertDom = view.render.call(helper.mvcContext(popoverOn), {
                alert: 'danger',
                caption: 'Failed with the following error(s)'
            }, popoverOn);

            var popoverWidget = require('../../../main/js/view/widgets/popover');

            var popover = popoverWidget.newPopover('Info Popover', alertDom, pageBody);
            popover.show();

//            helper.log(pageBody.html());

            expect($('.caption', pageBody).text()).toBe('Failed with the following error(s)');
            expect($('.errors', pageBody).length).toBe(0);
            expect($('.footer', pageBody).length).toBe(0);

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

            var popoverWidget = require('../../../main/js/view/widgets/popover');

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
