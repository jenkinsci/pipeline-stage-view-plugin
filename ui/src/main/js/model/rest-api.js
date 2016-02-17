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

var ajax = require('../util/ajax');

/**
 * Workflow REST API
 */

exports.getJobRuns = function(jobUrl, success, params) {
    ajax.execAsyncGET([jobUrl, 'wfapi', 'runs'], success, params);
}

exports.getDescription = function(of, success) {
    if (typeof of === 'string') {
        ajax.execAsyncGET([of], success);
    } else if (typeof of === 'object') {
        if (!of._links) {
            console.error("Request to get description of object where the object does not have a 'descUrl' property (the REST endpoint url).");
        } else {
            ajax.execAsyncGET([of._links.self.href], success);
        }
    } else {
        console.error("Request to get description using a type other than 'string' or 'object'.");
    }
}

exports.getPendingActions = function(url, success) {
    ajax.execAsyncGET([url], success);
}

exports.getObject = function(url, success) {
    ajax.execAsyncGET([url], success);
}
