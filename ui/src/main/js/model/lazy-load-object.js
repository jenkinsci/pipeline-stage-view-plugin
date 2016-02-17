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

var restApi = require('./rest-api');

exports.getModelData = function (callback) {
    var objectUrl = this.requiredAttr('objectUrl');

    // Lazy loading of an object.  The view code calls the getObject function on the model
    // it receives.  It allows the view to load the real model lazily e.g. only after
    // the user clicks on something.
    var lazyLoadModel = {
        getObject: function (loadCallback) {
            if (lazyLoadModel.cachedObject) {
                loadCallback(lazyLoadModel.cachedObject);
            } else {
                restApi.getObject(objectUrl, function (object) {
                    if (lazyLoadModel.cacheFunc) {
                        lazyLoadModel.cacheFunc(object);
                    }
                    loadCallback(object);
                });
            }
        }
    };

    return callback(lazyLoadModel);
}
