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

var url = require('./url');
var jqProxy = require('../jQuery');

exports.execAsyncGET = function (resPathTokens, success, params) {
    var $ = jqProxy.getJQuery();

    $.ajax({
        url: url.concatPathTokens(resPathTokens),
        type: 'get',
        dataType: 'json',
        data: params,
        cache: false, // Force caching off for IE (and anything else)
        success: success
    });
};

exports.jenkinsAjaxGET = function (path, success) {
    fetch(path, {
        cache: 'no-cache', // Force caching off for IE (and anything else)
    }).then((rsp) => {
        if (rsp.ok) {
            success();
        }
    });
};

exports.jenkinsAjaxPOST = function () {
    var path = arguments[0];
    if (arguments.length === 3) {
        var data = arguments[1];
        var success = arguments[2];
        if (typeof data !== 'string') {
            // TODO simplify when Prototype.js is removed
            data = Object.toJSON ? Object.toJSON(data) : JSON.stringify(data);
        }
        fetch(path, {
            method: 'post',
            headers: crumb.wrap({
                'Content-Type': 'application/json; charset=UTF-8'
            }),
            body: data,
        }).then((rsp) => {
            if (rsp.ok) {
                success();
            }
        });
    } else {
        var success = arguments[1];
        fetch(path, {
            method: 'post',
            headers: crumb.wrap({
                'Content-Type': 'application/json'
            }),
        }).then((rsp) => {
            if (rsp.ok) {
                success();
            }
        });
    }
};


exports.jenkinsAjaxPOSTURLEncoded = function (path, parameters, success) {
    fetch(path, {
        method: 'post',
        headers: crumb.wrap({
            'Content-Type': 'application/x-www-form-urlencoded'
        }),
        body: new URLSearchParams(parameters),
    }).then((rsp) => {
        if (rsp.ok) {
            success();
        }
    });
}
