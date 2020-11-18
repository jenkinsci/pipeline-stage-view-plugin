# JavaScript Testing

We're using [Jest] for defining tests. The jest script builds and runs these tests (which is also run from the main `mvn` build).

The easiest dev Pipeline regarding building and running tests is to run the `npm run mvntest -- --watch` command from `ui` folder.

```
~/cloudbees-workflow-plugin/ui$ npm run mvntest -- --watch
```

This task watches for both source code and test code changes:

* On a source code change:  Automatically runs all test specs.
* On test code change:  Automatically runs the changed test (only i.e. not all tests).

So best thing to do is keep a terminal window open and in view (some IDEs support an embedded terminal, which is very handy).  You'll be able to see your tests auto pass/fail as you make changes.


## Helper Module
Check out the [`helper.js`][helperjs] module for some test helper functions.

## Mocks
[Jest] provides very powerful mocking functionality. See the Jest docks for uses of `jest.mock` and `jest.fn`.

**NOTE:** mocks should always be cleaned up on an `afterEach` callback.
```
afterEach(() => {
    jest.resetModules();
    jest.resetAllMocks();
})
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

[Jest]: https://jestjs.io/
[helperjs]: ./helper.js
[testres]: ../resources
