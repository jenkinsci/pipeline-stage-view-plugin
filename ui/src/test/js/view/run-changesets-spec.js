/* jslint node: true */
/* global describe, it, expect */

"use strict";

var helper = require('../helper');

describe("view/run-changesets-spec", function () {

    it("- test_01", function (done) {
        helper.testWithJQuery('<div id="frag" objecturl="/jenkins/job/Job%20ABC/"></div>', function ($) {
            var run_changeset_model = helper.requireTestRes('model/run_changesets/01_run_changeset_post_model');
            var templates = helper.require('view/templates');

            var veiwFarg = templates.apply('run-changeset', run_changeset_model);

            var expectedFrag = helper.requireTestRes('view/run_changesets/expected_01.html');
//            helper.log('[' + veiwFarg.html() + ']');
//            helper.log('----------------------------------------------------------------------------');
//            helper.log('[' + expectedFrag + ']');
//            helper.compareMultilineText(veiwFarg.html(), expectedFrag);

            expect(veiwFarg.html()).toEqual(expectedFrag);

            done();
        });
    });

});