/*
 * Copyright (C) 2013 CloudBees Inc.
 *
 * All rights reserved.
 */

// Need to use js-modules to load the stage view from the pipeline-stage-view
// webapp dir (jsmodules). Adjuncts suck!!

var jsModules = require('jenkins-js-modules');
jsModules.addScript(
    'plugin/pipeline-stage-view/jsmodules/stageview.js',
    'jenkins-js-module:pipeline-stage-view:stageview:js'
);