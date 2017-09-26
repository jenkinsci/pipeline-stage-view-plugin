/*
 * Copyright (C) 2013 CloudBees Inc.
 *
 * All rights reserved.
 */

var mvc = require('./mvc');

// Register all controllers...
mvc.register(require('./controller/pipeline-staged'));
mvc.register(require('./controller/feature-tbd'));
mvc.register(require('./controller/stage-failed-popover'));
mvc.register(require('./controller/stage-actions-popover'));
mvc.register(require('./controller/stage-logs'));
mvc.register(require('./controller/node-log'));
mvc.register(require('./controller/run-input-required'));
mvc.register(require('./controller/build-artifacts-popup'));
mvc.register(require('./controller/run-changesets'));
mvc.register(require('./controller/remove-sidepanel'));
mvc.register(require('./controller/one-run-pipeline'));

// Apply controllers to the whole document.
var jqProxy = require('./jQuery');
var $ = jqProxy.getJQuery();
$(function() {
    if (isTestEnv()) {
        // In a test env, we do not want async rendering delays... just makes the
        // tests more complicated. Doing this for now and we'll see
        // if it's a good or bad idea :)
        require('./util/timeout').setMaxDelay(0);
    }
    mvc.applyControllers();
});

function isTestEnv() {
    if (window === undefined) {
        return true;
    } else if (window.navigator === undefined) {
        return true;
    } else if (window.navigator.userAgent === undefined) {
        return true;
    } else if (window.navigator.userAgent.toLowerCase().indexOf("phantomjs") !== -1) {
        return true;
    }
}

var stageView = require('./view/pipeline-staged.js');
var extpAPI = require('jenkins-js-extp/API');
var oneRun = require('./view/one-run-pipeline.js')
// Expose an API that will allow other plugins to contribute to the
// stage view. We make this exposed API "look" like that of 'jenkins-js-extp/API'
// so that we can mock an ExtensionPointContainer API on other plugins during dev,
// and then wire in the external/real Stage View ExtensionPointContainer instance
// at runtime.
module.exports = extpAPI;
// and overwrite the extensionPointContainer with the Stage View specific
// instance. This may eventually be a page level object, containing all
// extension points on the page.
module.exports.extensionPointContainer = stageView.extensionPointContainer;
module.exports.extensionPointContainer = oneRun.extensionPointContainer;
