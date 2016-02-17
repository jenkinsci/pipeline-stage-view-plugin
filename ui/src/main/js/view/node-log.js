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

exports.render = function (logLazyLoadModel, onElement) {
    // Add a caching function to the lazy load model.  This allows us to avoid
    // reloading of log details for completed nodes.
    logLazyLoadModel.cacheFunc = function(logDetails) {
        if (logDetails.nodeStatus === 'SUCCESS') {
            logLazyLoadModel.cachedObject = logDetails;
        }
    }

    onElement.click(function() {
        logLazyLoadModel.getObject(function (lazyLoad) {
            var nodeLogDom = templates.apply('node-log', lazyLoad);
            var $ = jqProxy.getJQuery();
            var logDetailsEl = $('.log-details', onElement);

            logDetailsEl.empty();
            logDetailsEl.append(nodeLogDom);
        });
    });
}
