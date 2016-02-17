# JavaScript Testing

We're using [Jasmine] for defining tests.  The gulp script builds and runs these tests (which is also run from the main `mvn` build).

The easiest dev Pipeline regarding building and running tests is to run the gulp `watch` task from `ui` folder.

```
tfennelly@diego:~/cloudbees-workflow-plugin/ui$ gulp watch
```

This task watches for both source code and test code changes:

* On a source code change:  Automatically rebuilds (bundles) the source and runs all test specs.
* On test code change:  Automatically runs the changed test (only i.e. not all tests).

So best thing to do is keep a terminal window open and in view (some IDEs support an embedded terminal, which is very handy).  You'll be able to see your tests auto pass/fail as you make changes.

If that's too noisy for you, simple run the `test` task.

```
tfennelly@diego:~/cloudbees-workflow-plugin/ui$ gulp test
```

## Helper Module
Check out the [`helper.js`][helperjs] module for some test helper functions.

## Spies (aka Mocks)
[Jasmine] provides very powerful "spying" (mocking etc) functionality called "Spies".  Mocking an exported function
function in one of the Pipeline modules would work as follows:

```
// Get the module...
var stageGroupedJobRuns = helper.require('model/stageGroupedJobRuns');

// Apply a callFake spy to the module instance i.e. mock the 'getModelData' function.
spyOn(stageGroupedJobRuns, 'getModelData').and.callFake(function(jobUrl, callback) {
    // mock code....
});
```

The [`helper.js`][helperjs] module provides an alternative to this that might be a bit less verbose (especially if you're already using
the `helper` module for logging or loading modules for testing).  It also allows you to mock multiple module functions in one call.

```
helper.spyOn('model/stageGroupedJobRuns', {
    getModelData: function (jobUrl, callback) {
        // mock code...
    }
});
```

## Test Resources (sample responses etc)
Test resources are often needed when (e.g.) mocking responses from a service.  The [`helper.js`][helperjs] module provides a helper
function for loading test resources from the [src/test/resources][testres] folder.

As an example, loading a sample response for [`model/stageGroupedJobRuns/getModelData/01_rest_api_jobHistory.json`](../resources/model/stageGroupedJobRuns/getModelData/01_rest_api_jobHistory.json)
works as follows:

```
var sampleRestApiJobHistory = helper.requireTestRes('model/stageGroupedJobRuns/getModelData/01_rest_api_jobHistory');
```

Note you don't need to include the `.json` file extension.

[Jasmine]: http://jasmine.github.io/2.0/introduction.html
[helperjs]: ./helper.js
[testres]: ../resources
