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

var KB = 1024;
var MB = KB * 1024;
var GB = MB * 1024;
var TB = GB * 1024;

var sec = 1000;
var min = sec * 60;
var hr  = min * 60;
var day = hr * 24;
var yr = day * 365;

exports.memory = function (amount) {
    if (amount > TB) {
        return (amount / TB).toFixed(2) + "TB";
    } else if (amount > GB) {
        return (amount / GB).toFixed(2) + "GB";
    } else if (amount > MB) {
        return (amount / MB).toFixed(2) + "MB";
    } else if (amount > KB) {
        return (amount / KB).toFixed(2) + "KB";
    } else {
        return amount + "B";
    }
}

exports.time = function (millis, numUnitsToShow) {
    if (millis <= 0) {
        return '0ms';
    }

    if (numUnitsToShow === undefined) {
        numUnitsToShow = 3;
    }

    function mod(timeUnit) {
        var numUnits = Math.floor(millis / timeUnit);
        millis = millis % timeUnit;
        return numUnits;
    }

    var years = mod(yr);
    var days = mod(day);
    var hours = mod(hr);
    var minutes = mod(min);
    var seconds = mod(sec);

    var numUnitsAdded = 0;
    var formattedTime = '';

    function addTimeUnit(value, unit) {
        if (numUnitsAdded === numUnitsToShow) {
            // don't add any more
            return;
        }
        if (value === 0 && numUnitsAdded === 0) {
            // Don't add a leading zero
            return;
        }

        // add this one.
        if (formattedTime === '') {
            formattedTime += value + unit;
        } else {
            formattedTime += ' ' + value + unit;
        }

        numUnitsAdded++;
    }

    addTimeUnit(years, 'y');
    addTimeUnit(days, 'd');
    addTimeUnit(hours, 'h');
    addTimeUnit(minutes, 'min');
    addTimeUnit(seconds, 's');
    // Only show millis if the time is below 1 second
    if (seconds === 0) {
        addTimeUnit(millis, 'ms');
    }

    return formattedTime;
}
