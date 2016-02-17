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

/**
 * Utility to manage timeouts, making it a bit easier to clear active timeouts. Useful for testing.
 */

var activeTimeoutHandlers = {};
var timeoutCounter = 0;
var maxDelay = Number.MAX_VALUE;

exports.newTimeoutHandler = function (handlerName) {
    if (!handlerName) {
        throw "The TimeoutHandler must be given a name.";
    }

    // Clear any timeouts on a handler by this name, if one exists.
    if (activeTimeoutHandlers.hasOwnProperty(handlerName)) {
        activeTimeoutHandlers[handlerName].clearAllTimeouts();
    }

    var timeoutHandler = new TimeoutHandler();
    activeTimeoutHandlers[handlerName] = timeoutHandler;
    return  timeoutHandler;
}

exports.clearAllTimeouts = function () {
    for (var handlerName in activeTimeoutHandlers) {
        if (activeTimeoutHandlers.hasOwnProperty(handlerName)) {
            activeTimeoutHandlers[handlerName].clearAllTimeouts();
        }
    }
}

exports.getMaxDelay = function () {
    return maxDelay;
}

exports.setMaxDelay = function (newMaxDelay) {
    maxDelay = newMaxDelay;
}

function TimeoutHandler() {
    this.timeouts = {};
    this.timeoutCount = 0;
}

TimeoutHandler.prototype.setTimeout = function(code, delay) {
    if (maxDelay === 0) {
        // There is no delay allowed (e.g. in tests). Run synchronously now.
        code();
        return undefined;
    } else {
        var thisHandler = this;

        function doTimeout(timeoutId) {
            var timeoutObj = setTimeout(function() {
                thisHandler.removeTimeout(timeoutId);
                code();
            }, Math.min(delay, maxDelay));
            thisHandler.timeouts[timeoutId] = timeoutObj;
        }

        var timeoutId = (timeoutCounter++).toString(10);
        doTimeout(timeoutId);
        this.timeoutCount++;

        return timeoutId;
    }
}

TimeoutHandler.prototype.activeTimeoutCount = function() {
    return this.timeoutCount;
}

TimeoutHandler.prototype.getTimeouts = function() {
    return this.timeouts;
}

TimeoutHandler.prototype.removeTimeout = function(timeoutId) {
    delete this.timeouts[timeoutId];
    this.timeoutCount--;
}

TimeoutHandler.prototype.clearTimeout = function(timeoutId) {
    var timeoutObj = this.timeouts[timeoutId];
    clearTimeout(timeoutObj);
    this.removeTimeout(timeoutId);
}

TimeoutHandler.prototype.clearAllTimeouts = function() {
    for (var timeoutId in this.timeouts) {
        this.clearTimeout(timeoutId);
    }
    this.timeouts = {};
    this.timeoutCount = 0;
}
