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

var cache = {};

function addCacheEntry (key, ob) {
    if (typeof ob === 'object') {
        if (ob.status && (ob.status === 'SUCCESS' || ob.status === 'ABORTED' || ob.status === 'FAILED' || ob.status === 'UNSTABLE')) {
            cache[key] = ob;
        }
    }
};

// Cache the stage data if it's eligible for caching
function cacheRunStages(run) {
    if (run && run.stages) {
        for (var j=0; j<run.stages.length; j++) {
            var stage = run.stages[j];
            try {
                addCacheEntry(stage._links.self.href, stage);
            } catch (e) {
                console.error("Stage with no link to it");
            }
        }
    }
}

exports.getJobRuns = function(jobUrl, success, params) {
    ajax.execAsyncGET([jobUrl, 'wfapi', 'runs'], function(obj) {
        // Cache the stages for the run
        for (var i=0; i < obj.length; i++) {
            cacheRunStages(obj[i]);
        }
        success(obj);
    }, params);
}

exports.getDescription = function(of, success) {
    var url;
    if (typeof of === 'string') {
        url = of;
    } else if (typeof of === 'object') {
        if (!of._links) {
            url = of._links.self.href;
        }
    } else {
        console.error("Request to get description using a type other than 'string' or 'object'.");
        return;
    }

    // Use existing cache if possible, otherwise make a request
    var cacheEntry = cache[url];
    if (cacheEntry) {
        success(cacheEntry);
    } else {
        ajax.execAsyncGET([url], function(ob) {
            // Cache the details for the object: this can greatly reduce the number of REST calls to the Jenkins backend.
            addCacheEntry(url, ob);
            success(ob);
        });
    }
}

exports.getPendingActions = function(url, success) {
    ajax.execAsyncGET([url], success);
}

exports.getObject = function(url, success) {
    ajax.execAsyncGET([url], success);
}
