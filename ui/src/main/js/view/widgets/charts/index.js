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

var jqProxy = require('../../../jQuery');

exports.stackedBarChart = function(values, highlightIdx) {
    if (highlightIdx !== undefined && highlightIdx < 0 || highlightIdx > (values.length - 1)) {
        throw '"highlightIdx" of ' + highlightIdx + ' is not valid for a value array of length ' + values.length;
    }

    var stackedBarChart = jqProxy.getJQuery()('<div class="stackedBarChart inner clearfix" />');
    var valueSum = 0;

    // Calc value sum
    for (var i = 0; i < values.length; i++) {
        valueSum += values[i];
    }

    // Calc percentages
    var percentages = [];
    for (var i = 0; i < values.length; i++) {
        percentages.push((values[i] / valueSum) * 100);
    }

    // Add cells based on percentages
    for (var i = 0; i < percentages.length; i++) {
        var cell = jqProxy.getJQuery()('<div class="bar" />');
        cell.css('width', percentages[i] + '%');
        if (highlightIdx !== undefined) {
            if (i < highlightIdx) {
                cell.addClass('prehighlight');
            } else if (i === highlightIdx) {
                cell.addClass('highlight');
            } else {
                cell.addClass('posthighlight');
            }
        }
        stackedBarChart.append(cell);

    }
    stackedBarChart.append('<div class="clearfix" />');

    return stackedBarChart;
}
