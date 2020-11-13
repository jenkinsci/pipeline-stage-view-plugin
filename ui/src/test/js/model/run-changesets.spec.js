/* jslint node: true */
/* global describe, it, expect */

"use strict";


describe("model/run-changesets-spec", function () {
    var helper;
    var model;

    var mockApi = jest.genMockFromModule('../../../main/js/model/rest-api');

    beforeEach(() => {
        helper = require('../helper');
        jest.mock('../../../main/js/model/rest-api', () => mockApi)
        model = require('../../../main/js/model/run-changesets');
    })

    afterEach(() => {
        jest.resetModules();
        jest.resetAllMocks();
    })

    it("- test_01_getModelData", function (done) {
        helper.testWithJQuery('<div objectUrl="job/blah/123/wfapi/changesets"></div>', function ($) {
            var targetEl = $('div');

            mockApi.getObject.mockImplementation((restPathTokens, callback) => {
                    var _01_run_changeset_pre_model = helper.requireTestRes('model/run_changesets/01_run_changeset_pre_model');
                    callback(_01_run_changeset_pre_model);
            })

            model.getModelData.call(
                helper.mvcContext(targetEl),
                function (transformedModelData) {
                    var _01_run_changeset_post_model_expected = helper.requireTestRes('model/run_changesets/01_run_changeset_post_model');
    //                helper.log(transformedModelData);
                    expect(transformedModelData).toEqual(_01_run_changeset_post_model_expected);

                    done();
                }
            );

        });
    });

});
