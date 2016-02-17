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

exports.render = function (menuConfig, onElement) {
    var $ = jqProxy.getJQuery();
    var menuPopoutWidget = templates.apply('menu-popout', menuConfig, false);
    var menuItems = $('.cbwf-menu-items', menuPopoutWidget);
    var popoverName = this.getControllerName();

    // remove the menu items from the menuPopoutWidget, just leaving it with
    // the menu icon.  The menu items will be popped out from the
    // menuPopoutWidget when it is hovered.
    menuItems.remove();

    // Display the menuPopoutWidget when the mouse hovers the target element.
    var popover = popoverWidget.newPopover(popoverName, menuPopoutWidget, onElement.parent(), {
            placement: menuConfig['click-widget-placement'],
            onshow: function() {
                // Display the menu items when the mouse hovers the menuPopoutWidget.
                popoverWidget.hover(menuItems, menuPopoutWidget, {
                    placement: menuConfig['menu-items-placement'],
                    hoverBoth: true
                });
            }
        }
    );
    popover.hover();
}
