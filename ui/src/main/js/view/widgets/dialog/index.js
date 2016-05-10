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
var templates = require('../../templates');
var popoverWidget = require('../popover');

exports.show = function(title, body, options) {
    if (!options) {
        options = {};
    }

    var dialog = templates.apply('dialog', {
        title: title,
        options: options
    });

    var $ = jqProxy.getJQuery();
    var theWindow = require('window-handle').getWindow();
    var headerEl = $('.header', dialog);
    var bodyEl = $('.body', dialog);

    if (options.width) {
        dialog.css('width', options.width);
    } else {
        var winWidth = $(theWindow).width();
        var popoverWidth = Math.min(800, (winWidth * 0.7));
        dialog.css('max-width', popoverWidth);
    }
    if (options.height) {
        dialog.css('height', options.height);
    } else {
        var winHeight = $(theWindow).height();
        var popoverHeight = Math.min(600, (winHeight * 0.7));
        dialog.css('max-height', popoverHeight);
    }

    bodyEl.append(body);

    options.modal = true;
    // Undefined for the onElement makes it in the window center, otherwise it needs it for positioning
    var popover = popoverWidget.newPopover(title, dialog, options.onElement, options);

    popover.show();
    if (options.onshow) {
        options.onshow();
    }

    return popover;
}

exports.hide = function(dialog) {
    popoverWidget.hide(dialog);
}
