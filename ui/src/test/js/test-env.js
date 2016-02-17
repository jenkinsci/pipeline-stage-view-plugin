/* jslint node: true */
/* global describe, it, expect */

"use strict";

/**
 * Test environment setup.
 * <p/>
 * E.g. clear all modules from the require cache.
 */

describe("test environment setup", function () {

    // Zap all workflow related source/test modules from the require cache, forcing
    // everything to be reloaded on require. This will not effect mocking/spying
    // because we only do it once before running the actual test specs.

    for (var moduleId in require.cache) {
        if (moduleId.indexOf('cloudbees-workflow-plugin/ui/src/') !== -1) {
            delete require.cache[moduleId];
        }
    }
});
