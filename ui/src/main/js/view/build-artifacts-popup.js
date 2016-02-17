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

var jqProxy = require('../jQuery');
var templates = require('./templates');
var popoverWidget = require('./widgets/popover');

exports.render = function (lazyLoadConfig, onElement) {
    var $ = jqProxy.getJQuery();
    var popupEl = $('<div class="cbwf-build-artifacts"></div>');

    var popover = popoverWidget.newPopover("build-artifacts", popupEl, onElement, {
        placement: 'right',
        hoverBoth: true,
        namespace: 'build-artifacts',
        onshow: function() {
            lazyLoadConfig.getObject(function (lazyLoad) {
                var lazyLoadToDisplay = templates.apply('build-artifacts', lazyLoad);
                popupEl.empty().append(lazyLoadToDisplay);

                // need to reapply placement of the popover because:
                // 1. we have altered it's content, which possibly widened it
                // 2. it is positioned to the left and so, if widened, means it's positioning is off
                popover.applyPlacement();
            });
        }
    });

    popover.hover();
}
