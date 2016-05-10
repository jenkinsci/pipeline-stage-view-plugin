/* jslint node: true */
/* global describe, it, expect */

"use strict";

var formatters = require('../helper').require('util/formatters');

var sec = 1000;
var min = sec * 60;
var hr  = min * 60;
var day = hr * 24;
var yr = day * 365;

describe("util/formatters-spec", function () {

    it("- test_memory", function () {
        expect(formatters.memory(1)).toBe('1B');
        expect(formatters.memory(999)).toBe('999B');
        expect(formatters.memory(1999)).toBe('1.95KB');
        expect(formatters.memory(1999999)).toBe('1.91MB');
        expect(formatters.memory(1999999999)).toBe('1.86GB');
        expect(formatters.memory(1999999999999)).toBe('1.82TB');
    });

    it("- test_time_default", function () {

        // By default, the formatter should only show the 3 most significant units of time that have value.

        var fiveYears_40days_6hours_20Mins_18Secs_234Millis = (yr*5) + (day*40) + (hr*6) + (min*20) + (sec*18) + 234;
        expect(formatters.time(fiveYears_40days_6hours_20Mins_18Secs_234Millis)).toBe('5y 40d 6h');

        var fourtyDays_6hours_20Mins_18Secs_234Millis = (day*40) + (hr*6) + (min*20) + (sec*18) + 234;
        expect(formatters.time(fourtyDays_6hours_20Mins_18Secs_234Millis)).toBe('40d 6h 20min');

        var sixHours_20Mins_18Secs_234Millis = (hr*6) + (min*20) + (sec*18) + 234;
        expect(formatters.time(sixHours_20Mins_18Secs_234Millis)).toBe('6h 20min 18s');

        // the formatter should only show millis when the duration is less than one second.

        var twentyMins_18Secs_234Millis = (min*20) + (sec*18) + 234;
        expect(formatters.time(twentyMins_18Secs_234Millis)).toBe('20min 18s');

        var eighteenSecs_234Millis = (sec*18) + 234;
        expect(formatters.time(eighteenSecs_234Millis)).toBe('18s');

        var _234Millis = 234;
        expect(formatters.time(_234Millis)).toBe('234ms');
    });

    it("- test_time_num_units_displayed", function () {

        var fiveYears_40days_6hours_20Mins_18Secs_234Millis = (yr*5) + (day*40) + (hr*6) + (min*20) + (sec*18) + 234;
        expect(formatters.time(fiveYears_40days_6hours_20Mins_18Secs_234Millis, 2)).toBe('5y 40d');
        expect(formatters.time(fiveYears_40days_6hours_20Mins_18Secs_234Millis, 1)).toBe('5y');
    });

    it("- test_time_negative", function () {
        expect(formatters.time(-1, 2)).toBe('0ms');
    });

});
