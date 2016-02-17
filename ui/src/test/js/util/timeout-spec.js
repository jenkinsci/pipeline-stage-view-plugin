/* jslint node: true */
/* global describe, it, expect */

"use strict";

var helper = require('../helper');

describe("util/timeout-spec", function () {
    
    beforeEach(function() {
        helper.clearRequireCache('util/timeout');
    });
    
    function assertHandlerCleared(handler) {
         expect(handler.activeTimeoutCount()).toBe(0);
        
        // all timeout objects should have been removed / cleaned up
        var timeouts = handler.getTimeouts();
        var idCount = 0;
        for (var timeoutId in timeouts) {
            if (timeouts.hasOwnProperty(timeoutId)) {
                idCount++;
            }
        }
        expect(idCount).toBe(0);
    }

    it("- test_construction", function () {
        var timeoutModule = helper.require('util/timeout');

        // must define a name
        try {
            timeoutModule.newTimeoutHandler();
            throw "TimeoutHandler should have thrown an exception for not providing a name";
        } catch (e) {
            // correct
        }
        try {
            timeoutModule.newTimeoutHandler('');
            throw "TimeoutHandler should have thrown an exception for not providing a name";
        } catch (e) {
            // correct
        }

        var handler = timeoutModule.newTimeoutHandler('aname');
        expect(handler).toBeDefined();
    });

//    it("- test_uncanceled_timeouts", function (done) {
//        var timeoutModule = helper.require('util/timeout');
//
//        // test the normal use case
//        var handler = timeoutModule.newTimeoutHandler('aname1');
//
//        // In case max was set to 0 i.e. sync mode Vs async
//        timeoutModule.setMaxDelay(Number.MAX_VALUE);
//
//        var execCount = 0;
//        var counterFunc = function() {
//            execCount++;
//        }
//
//        // Set a few timeouts.
//        handler.setTimeout(counterFunc, 200);
//        handler.setTimeout(counterFunc, 200);
//        handler.setTimeout(counterFunc, 200);
//
//        // There should be 3 active now.
//        expect(handler.activeTimeoutCount()).toBe(3);
//        handler.setTimeout(function() {
//            try {
//                // timeouts should have all executed
//                expect(execCount).toBe(3);
//                assertHandlerCleared(handler);
//            } finally {
//                done();
//            }
//        }, 500);
//    });


//    it("- test_canceled_timeouts", function (done) {
//        var timeoutModule = helper.require('util/timeout');
//       
//        // test the normal use case
//        var handler = timeoutModule.newTimeoutHandler('aname2');
//
//        // In case max was set to 0 i.e. sync mode Vs async
//        timeoutModule.setMaxDelay(Number.MAX_VALUE);
//
//        var execCount = 0;
//        var counterFunc = function() {
//            execCount++;
//        }
//
//        // Set a few timeouts.
//        handler.setTimeout(counterFunc, 300);
//        handler.setTimeout(counterFunc, 300);
//        handler.setTimeout(counterFunc, 300);
//
//        // There should be 3 active now.
//        expect(handler.activeTimeoutCount()).toBe(3);
//
//        // Immediately clear all the timeouts before they get to execute
//        timeoutModule.clearAllTimeouts();
//        // There should be 0 active now (all cleared).
//        expect(handler.activeTimeoutCount()).toBe(0);
//
//        handler.setTimeout(function() {
//            try {
//                // timeouts should not have executed
//                expect(execCount).toBe(0);
//                assertHandlerCleared(handler);
//            } finally {
//                done();
//            }
//        }, 500);
//    });
});
