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

var mvc = require('../../mvc');
var handlebars = require('handlebars');
var jqProxy = require('../../jQuery');
var formatters = require('../../util/formatters');
var moment = require('moment');

/**
 * Templating support.
 */

// Create the template cache..
var templateCache = {
    'info-action-popover': require('./info-action-popover.hbs'),
    'dialog': require('../widgets/dialog/template.hbs'),
    'one-run-pipeline': require('./one-run-pipeline.hbs'),
    'pipeline-staged': require('./pipeline-staged.hbs'),
    'menu-popout': require('./menu-popout.hbs'),
    'stage-logs': require('./stage-logs.hbs'),
    'node-log': require('./node-log.hbs'),
    'run-input-required': require('./run-input-required.hbs'),
    'run-input-required-redirect': require('./run-input-required-redirect.hbs'),
    'build-artifacts': require('./build-artifacts.hbs'),
    'run-changesets': require('./run-changesets.hbs'),
    'run-changeset': require('./run-changeset.hbs')
};

// Initialise handlebars with helpers

var dateFormattingOn = true;
var formatAliases = {
    short: 'HH:mm (MMM DD)',
    month: 'MMM',
    dom: 'DD',
    time: 'HH:mm',
    ISO_8601: 'YYYY-MM-DDTHH:mm:ss',
    long: this.ISO_8601
};

function registerHBSHelper(name, helper) {
    handlebars.registerHelper(name, helper);
}

registerHBSHelper('breaklines', function(text) {
    text = handlebars.escapeExpression(text);
    text = text.replace(/(\r\n|\n|\r)/gm, '<br>');
    return new handlebars.SafeString(text);
});

registerHBSHelper('dumpObj', function(object) {
    return JSON.stringify(object, undefined, 4);
});

registerHBSHelper('formatMem', function(amount) {
    return formatters.memory(amount);
});

registerHBSHelper('formatTime', function(millis, numUnitsDisplayed) {
    return formatters.time(millis, numUnitsDisplayed);
});

registerHBSHelper('formatDate', function (date, toFormat) {
    if (!dateFormattingOn) {
        // Just return as is...
        return date;
    }

    var aliasFormat = formatAliases[toFormat];
    if (aliasFormat) {
        return moment(date).format(aliasFormat);
    } else {
        return moment(date).format(toFormat);
    }
});

/**
 * A simple helper that converts an emphasise value (between 1.0 and 1.5) to an opacity value
 * between 0.5 and 1.0.
 */
registerHBSHelper('emphasiseToOpacity', function(emphasiseVal) {
    // first make sure the value is between 1 and 1.5
    emphasiseVal = Math.max(emphasiseVal, 1.0);
    emphasiseVal = Math.min(emphasiseVal, 1.5);

    // convert to opacity by just subtracting 0.5 :)
    return (emphasiseVal - 0.5);
});

registerHBSHelper('ifCond', function (v1, operator, v2, options) {
    switch (operator) {
        case '==':
            return (v1 === v2) ? options.fn(this) : options.inverse(this);
        case '===':
            return (v1 === v2) ? options.fn(this) : options.inverse(this);
        case '!=':
            return (v1 !== v2) ? options.fn(this) : options.inverse(this);
        case '!==':
            return (v1 !== v2) ? options.fn(this) : options.inverse(this);
        case '<':
            return (v1 < v2) ? options.fn(this) : options.inverse(this);
        case '<=':
            return (v1 <= v2) ? options.fn(this) : options.inverse(this);
        case '>':
            return (v1 > v2) ? options.fn(this) : options.inverse(this);
        case '>=':
            return (v1 >= v2) ? options.fn(this) : options.inverse(this);
        case '&&':
            return (v1 && v2) ? options.fn(this) : options.inverse(this);
        case '||':
            return (v1 || v2) ? options.fn(this) : options.inverse(this);
        default:
            return options.inverse(this);
    }
});

function getTemplate(templateName) {
    var templateInstance = templateCache[templateName];
    if (!templateInstance) {
        throw 'No template by the name "' + templateName + '".  Check ui/src/main/js/view/templates/index.js and make sure the template is registered in the templateCache.';
    }
    return  templateInstance;
}

/**
 * Get a template from the template cache.
 * @param templateName The template name.
 * @returns The template instance.
 */
exports.get = function (templateName) {
    return  getTemplate(templateName);
}

/**
 * Apply the named template to the provided data model.
 * @param templateName The name of the template.
 * @param dataModel The data model to which the template is to be applied.
 * @param divWrap Flag indicating whether the templating result is to be wrapped in a div
 * element (default true).  Needed if the html produced by the template contains text nodes
 * at the root level.
 * @returns jQuery DOM.
 */
exports.apply = function (templateName, dataModel, divWrap) {
    var templateInstance = getTemplate(templateName);
    var html = templateInstance(dataModel);
    var jQueryDom;

    if (divWrap === undefined || divWrap) {
        jQueryDom = jqProxy.getJQuery()('<div>' + html + '</div>');
    } else {
        jQueryDom = jqProxy.getJQuery()(html);
    }

    // Apply all controllers before returning...
    mvc.applyControllers(jQueryDom);

    return jQueryDom;
}

/**
 * Turn template date formatting on/off.
 * <p/>
 * Useful for testing.
 * @param on Formatting on off flag.  True to turn formatting on (default),
 * otherwise false.
 */
exports.dateFormatting = function (on) {
    dateFormattingOn = on;
}
