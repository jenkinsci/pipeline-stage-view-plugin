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

var gutil = require('gulp-util');
var fs = require('fs');
var requireUncached = require("require-uncached");

Object.toJSON = function(object) {
    return JSON.stringify(object);
}

/**
 * require one of the pipeline source modules.
 * <p/>
 * Takes care of resolving relative paths etc.
 *
 * @param module The module name.
 * @returns The module instance.
 */
exports.require = function (module) {
    return require('../../main/js/' + module);
}

exports.clearRequireCache =  function(module) {
    if (!module) {
        throw "No 'module' specified.";
    }

    for (var moduleId in require.cache) {
        if (moduleId.indexOf(module) !== -1) {
            delete require.cache[moduleId];
        }
    }
}

/**
 * require a test resource.
 * <p/>
 * Gets them from the test/resources dir.
 * @param resource The resource path.
 * @returns The resource instance.
 */
exports.requireTestRes = function (resource) {
    if (endsWith(resource, '.html') || endsWith(resource, '.txt')) {
        return fs.readFileSync('./src/test/resources/' + resource, "utf-8");
    } else {
        return requireUncached('../resources/' + resource);
    }
}

/**
 * Log a message to the console using Gulp logger.
 * @param message The message to log.
 */
exports.log = function (message) {
    if (typeof message === 'object') {
        gutil.log(JSON.stringify(message, undefined, 4));
    } else {
        gutil.log(message);
    }
}

/**
 * Log an error to the console using Gulp logger.
 * <p/>
 * Colorizes the log.
 * @param message The error message to log.
 */
exports.error = function (message) {
    gutil.log(gutil.colors.red(message));
    gutil.log(gutil.colors.red(new Error().stack));
}

/**
 * Mock a set of functions on the specified module.
 * @param module The module to mock.  The name of the module, or the module instance.
 * @param mocks An object containing the mocks.
 */
exports.spyOn = function (module, mocks) {
    var moduleInstance;

    if (typeof module === 'string') {
        // It's the module name
        moduleInstance = this.require(module);
    } else if (typeof module === 'object') {
        // It's the resolved module instance
        moduleInstance = module;
    }

    // apply the mocks....
    for (var propToMock in mocks) {
        if (mocks.hasOwnProperty(propToMock)) {
            jasmine.getEnv().spyOn(moduleInstance, propToMock).and.callFake(mocks[propToMock]);
        }
    }
}

exports.trim = function(str) {
    var jqProxy = exports.require('./jQuery');
    var $ = jqProxy.getJQuery();

    return $.trim(str);
}

exports.compareMultilineText = function(text1, text2, trimLines) {
    var text1Lines = text1.split(/^/m);
    var text2Lines = text2.split(/^/m);

    if (text1Lines.length !== text2Lines.length) {
        console.log("Text1 has " + text1Lines.length + " lines of text, while Text2 has " + text2Lines.length);
        return false
    }

    for (var i = 0; i < text1Lines.length; i++) {
        var text1Line = text1Lines[i];
        var text2Line = text2Lines[i];

        if (trimLines) {
            text1Line = exports.trim(text1Line);
            text2Line = exports.trim(text2Line);
        }

        if (text1Line !== text2Line) {
            console.error("Texts do not match at line number " + (i + 1) + ".\n\t[" + text1Line + "](" + text1Line.length + " chars)\n\t[" + text2Line + "](" + text2Line.length + " chars)");
            return false;
        }
    }

    console.log("Texts match");
    return true;
}

/**
 * An alias for the {@link spyOn} function.
 */
exports.mock = exports.spyOn;

/**
 * Create a mock MVC config element.
 * <p/>
 * Just needs to look a bit like a jQuery object so we can get attributes off it.
 * @param config The config object.
 */
exports.mockControllerConfigElement = function(config) {
    config.attr = function(attributeName) {
        return config[attributeName];
    }
    return config;
}

exports.mvcContext = function(element) {
    var mvc = exports.require('./mvc');
    return mvc.newContext('mock-controller', element);
}

exports.mvcContextWithMockConfig = function(config) {
    var mockConfigEl = exports.mockControllerConfigElement(config);
    var mvc = exports.require('./mvc');
    return mvc.newContext('mock-controller', mockConfigEl);
}

exports.mvcRegister = function(controllers) {
    var mvc = exports.require('./mvc');

    function register(controllerName) {
        if (!mvc.isRegistered(controllerName)) {
            var controllerModule = exports.require('./controller/' + controllerName);
            mvc.register(controllerModule);
        }
    }

    if (typeof controllers === 'string') {
        // just one controller name in a string
        register(controllers);
    } else {
        // it's an array of controller names
        for (var i = 0; i < controllers.length; i++) {
            register(controllers[i]);
        }
    }

    return mvc;
}

exports.mvcRun = function(controllers, applyOnElement) {
    var mvc = exports.mvcRegister(controllers);

    if (applyOnElement) {
        mvc.applyControllers(applyOnElement)
    }
}

exports.testWithJQuery = function (content, testFunc) {
    var jsdom = require('jsdom');

    jsdom.env({
        html: content,
        done: function (err, window) {
            require('window-handle').setWindow(window);

            var timeoutModule = exports.require('util/timeout');

            // set the max delay to zero (exec immediately/synchronously) so the
            // tests don't run into render delay issues
            timeoutModule.setMaxDelay(0);

            try {
                var jQD = require('jquery-detached');
                testFunc(jQD.getJQuery());
            } catch (e) {
                exports.error(e);
            } finally {
                timeoutModule.clearAllTimeouts();
            }
        }
    });
}

function endsWith(string, value) {
    if (string.length < value.length) {
        return false;
    }
    return (string.slice(0 - value.length) === value);
}
