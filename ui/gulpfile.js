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
var builder = require('jenkins-js-builder');

//
// Bundle the modules.
// See https://github.com/jenkinsci/js-builder#bundling
//
// We need 2 bundles:
// 1. And adjunct so we can load things from .jelly. It then uses
//    jsmodules to load the jsbundle (below). Grrr I hate adjuncts.
// 2. A jsbundle so other bundles can import it.
//
builder.bundle('src/main/js/stageview_adjunct.js')
    .less('src/main/less/stageview_adjunct.less')
    .minify()
    .inDir('target/generated-adjuncts/org/jenkinsci/pipeline');
builder.bundle('src/main/js/stageview.js')
    .withExternalModuleMapping('jquery-detached', 'jquery-detached:jquery2')
    .withExternalModuleMapping('handlebars', 'handlebars:handlebars3')
    .withExternalModuleMapping('moment', 'momentjs:momentjs2')
    .asJenkinsModuleResource()
    .export();

// Explicitly setting the task list so as to disable jshint
// TODO: Remove the line below + fix jshint build errors
builder.defineTasks(['test', 'bundle', 'rebundle']);
builder.defineTask('lint', function() {}); // https://github.com/jenkinsci/plugin-pom/pull/20#issuecomment-206374472
