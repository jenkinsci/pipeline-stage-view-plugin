/* jslint node: true */
/* global describe, it, expect */

"use strict";

var helper = require('../helper');
var model = helper.require('model/run-changesets');

describe("model/run-changesets-spec", function () {

    it("- test_01_getModelData", function (done) {
        helper.testWithJQuery('<div objectUrl="job/blah/123/wfapi/changesets"></div>', function ($) {
            var targetEl = $('div');

            helper.mock('model/rest-api', {
                getObject: function (resPathTokens, callback) {
                    var _01_run_changeset_pre_model = helper.requireTestRes('model/run_changesets/01_run_changeset_pre_model');
                    callback(_01_run_changeset_pre_model);
                }
            });

            model.getModelData.call(
                helper.mvcContext(targetEl),
                function (transformedModelData) {
                    var _01_run_changeset_post_model_expected = helper.requireTestRes('model/run_changesets/01_run_changeset_post_model');
    //                helper.log(transformedModelData);
                    expect(transformedModelData).toEqual(_01_run_changeset_post_model_expected);
                }
            );

            done();
        });
    });

});
