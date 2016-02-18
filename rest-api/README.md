# Pipeline REST API Plugin

This module is a Jenkins Plugin that defines REST endpoints for securely accessing Pipeline data that can then be used
in (e.g.) the Pipeline Stage View.

## Resource Hyperlinks

As you use the API you will notice that resources are hyperlinked, allowing client applications to navigate the API
once they know the root/base URL for the API i.e. without needing to know how to construct other resource URLs.

We try to follow the [Hypertext Application Language (HAL)][HAL] draft standard as much as possible for resource hyperlinking.
As such, most resources will have a `_links` object containing hyperlinks to relevant resource e.g.
the `_links` on a Pipeline run might appear as follows:

```json
"_links": {
    "self": {
        "href": "/jenkins/job/Test%20Workflow/15/wfapi/describe"
    },
    "artifacts": {
        "href": "/jenkins/job/Test%20Workflow/15/wfapi/artifacts"
    }
}
```

## Available REST Endpoints

The following is a list of Pipeline REST API Endpoints.

### GET /job/:`job-name`/wfapi

GET Pipeline job description.

```json
{
    "_links": {
        "self": {
            "href": "/jenkins/job/Test%20Workflow/wfapi/describe"
        },
        "runs": {
            "href": "/jenkins/job/Test%20Workflow/wfapi/runs"
        }
    },
    "name": "Test Workflow",
    "runCount": 17
}
```

### GET /job/:`job-name`/wfapi/runs

GET Pipeline job run History.

Sample Response:

```json
[
    {
        "_links": {
            "self": {
                "href": "/jenkins/job/Test%20Workflow/16/wfapi/describe"
            },
            "pendingInputActions": {
                "href": "/jenkins/job/Test%20Workflow/16/wfapi/pendingInputActions"
            }
        },
        "id": "2014-10-16_13-07-52",
        "name": "#16",
        "status": "PAUSED_PENDING_INPUT",
        "startTimeMillis": 1413461275770,
        "endTimeMillis": 1413461285999,
        "durationMillis": 10229,
        "stages": [
            {
                "_links": {
                    "self": {
                        "href": "/jenkins/job/Test%20Workflow/16/execution/node/5/wfapi/describe"
                    }
                },
                "id": "5",
                "name": "Build",
                "status": "SUCCESS",
                "startTimeMillis": 1413461275770,
                "durationMillis": 5228
            },
            {
                "_links": {
                    "self": {
                        "href": "/jenkins/job/Test%20Workflow/16/execution/node/8/wfapi/describe"
                    }
                },
                "id": "8",
                "name": "Test",
                "status": "SUCCESS",
                "startTimeMillis": 1413461280998,
                "durationMillis": 4994
            },
            {
                "_links": {
                    "self": {
                        "href": "/jenkins/job/Test%20Workflow/16/execution/node/10/wfapi/describe"
                    }
                },
                "id": "10",
                "name": "Deploy",
                "status": "PAUSED_PENDING_INPUT",
                "startTimeMillis": 1413461285992,
                "durationMillis": 7
            }
        ]
    },
    {
        "_links": {
            "self": {
                "href": "/jenkins/job/Test%20Workflow/15/wfapi/describe"
            },
            "artifacts": {
                "href": "/jenkins/job/Test%20Workflow/15/wfapi/artifacts"
            }
        },
        "id": "2014-10-16_12-45-06",
        "name": "#15",
        "status": "SUCCESS",
        "startTimeMillis": 1413459910289,
        "endTimeMillis": 1413459937070,
        "durationMillis": 26781,
        "stages": [
            {
                "_links": {
                    "self": {
                        "href": "/jenkins/job/Test%20Workflow/15/execution/node/5/wfapi/describe"
                    }
                },
                "id": "5",
                "name": "Build",
                "status": "SUCCESS",
                "startTimeMillis": 1413459910289,
                "durationMillis": 6754
            },
            {
                "_links": {
                    "self": {
                        "href": "/jenkins/job/Test%20Workflow/15/execution/node/8/wfapi/describe"
                    }
                },
                "id": "8",
                "name": "Test",
                "status": "SUCCESS",
                "startTimeMillis": 1413459917043,
                "durationMillis": 4998
            },
            {
                "_links": {
                    "self": {
                        "href": "/jenkins/job/Test%20Workflow/15/execution/node/10/wfapi/describe"
                    }
                },
                "id": "10",
                "name": "Deploy",
                "status": "SUCCESS",
                "startTimeMillis": 1413459922041,
                "durationMillis": 15029
            }
        ]
    }
]
```

See next section re how to get details of a single run.

### GET /job/:`job-name`/:`run-id`/wfapi/describe

Get a single Workflow run.

```json
{
    "_links": {
        "self": {
            "href": "/jenkins/job/Test%20Workflow/16/wfapi/describe"
        },
        "pendingInputActions": {
            "href": "/jenkins/job/Test%20Workflow/16/wfapi/pendingInputActions"
        }
    },
    "id": "2014-10-16_13-07-52",
    "name": "#16",
    "status": "PAUSED_PENDING_INPUT",
    "startTimeMillis": 1413461275770,
    "endTimeMillis": 1413461285999,
    "durationMillis": 10229,
    "stages": [
        {
            "_links": {
                "self": {
                    "href": "/jenkins/job/Test%20Workflow/16/execution/node/5/wfapi/describe"
                }
            },
            "id": "5",
            "name": "Build",
            "status": "SUCCESS",
            "startTimeMillis": 1413461275770,
            "durationMillis": 5228
        },
        {
            "_links": {
                "self": {
                    "href": "/jenkins/job/Test%20Workflow/16/execution/node/8/wfapi/describe"
                }
            },
            "id": "8",
            "name": "Test",
            "status": "SUCCESS",
            "startTimeMillis": 1413461280998,
            "durationMillis": 4994
        },
        {
            "_links": {
                "self": {
                    "href": "/jenkins/job/Test%20Workflow/16/execution/node/10/wfapi/describe"
                }
            },
            "id": "10",
            "name": "Deploy",
            "status": "PAUSED_PENDING_INPUT",
            "startTimeMillis": 1413461285992,
            "durationMillis": 7
        }
    ]
}
```

Note that if a run has a status of `PAUSED_PENDING_INPUT` it will also have a `pendingInputActions` hyperlink,
which can be used to navigate to the pending input actions resource.

### GET /job/:`job-name`/:`run-id`/wfapi/pendingInputActions

Get the pending input action details for a Pipeline run.

Sample Response:

```json
[
    {
        "id": "Ef95dd500ae6ed3b27b89fb852296d12",
        "message": "Is the build okay?",
        "proceedUrl": "/jenkins/job/Test%20Workflow/11/input/Ef95dd500ae6ed3b27b89fb852296d12/proceedEmpty",
        "abortUrl": "/jenkins/job/Test%20Workflow/11/input/Ef95dd500ae6ed3b27b89fb852296d12/abort"
    }
]
```

### GET /job/:`job-name`/:`run-id`/wfapi/artifacts

Get the build artifacts for a Pipeline run.

The `/job/:job-name/wfapi/runs` and `/job/:job-name/:run-id/wfapi/describe` endpoints will return an `artifacts`
hyperlink on Pipeline runs if the given run archived any artifacts for that run. Hitting that endpoint URL
gets you a list of the artifacts.

Sample Response:

```json
[
    {
        "id": "n3",
        "name": "hello2.jar",
        "path": "target/hello2.jar",
        "url": "/jenkins/job/Test%20Workflow/14/artifact/target/hello2.jar",
        "size": 6
    },
    {
        "id": "n4",
        "name": "simple-maven-project-with-tests-1.0-SNAPSHOT.jar",
        "path": "target/simple-maven-project-with-tests-1.0-SNAPSHOT.jar",
        "url": "/jenkins/job/Test%20Workflow/14/artifact/target/simple-maven-project-with-tests-1.0-SNAPSHOT.jar",
        "size": 2467
    }
]
```

### GET /job/:`job-name`/:`run-id`/execution/node/:`node-id`/wfapi/describe

Get a description of a Pipeline node.

If the node is a stage step, the response will have a list of all the Pipeline nodes that were active during
that stage, ordered by node start time.  Use the `parentNodes` references for navigation of the stage
flow nodes.

Sample Response (successfully executed stage):

```json
{
    "_links": {
        "self": {
            "href": "/jenkins/job/Test%20Workflow/16/execution/node/5/wfapi/describe"
        }
    },
    "id": "5",
    "name": "Build",
    "status": "SUCCESS",
    "startTimeMillis": 1413461275770,
    "durationMillis": 5228,
    "stageFlowNodes": [
        {
            "_links": {
                "self": {
                    "href": "/jenkins/job/Test%20Workflow/16/execution/node/6/wfapi/describe"
                },
                "log": {
                    "href": "/jenkins/job/Test%20Workflow/16/execution/node/6/wfapi/log"
                }
            },
            "id": "6",
            "name": "Git",
            "status": "SUCCESS",
            "startTimeMillis": 1413461275783,
            "durationMillis": 9,
            "parentNodes": [
                "5"
            ]
        },
        {
            "_links": {
                "self": {
                    "href": "/jenkins/job/Test%20Workflow/16/execution/node/7/wfapi/describe"
                },
                "log": {
                    "href": "/jenkins/job/Test%20Workflow/16/execution/node/7/wfapi/log"
                }
            },
            "id": "7",
            "name": "Shell Script",
            "status": "SUCCESS",
            "startTimeMillis": 1413461275792,
            "durationMillis": 5206,
            "parentNodes": [
                "6"
            ]
        }
    ]
}
```

Sample Response (unsuccessfully executed stage):

```json
{
    "_links": {
        "self": {
            "href": "/jenkins/job/Test%20Workflow/17/execution/node/8/wfapi/describe"
        }
    },
    "id": "8",
    "name": "Test",
    "status": "FAILED",
    "startTimeMillis": 1413462240994,
    "durationMillis": 5030,
    "stageFlowNodes": [
        {
            "_links": {
                "self": {
                    "href": "/jenkins/job/Test%20Workflow/17/execution/node/9/wfapi/describe"
                },
                "log": {
                    "href": "/jenkins/job/Test%20Workflow/17/execution/node/9/wfapi/log"
                }
            },
            "id": "9",
            "name": "Shell Script",
            "status": "FAILED",
            "error": {
                "message": "script returned exit code 1",
                "type": "hudson.AbortException"
            },
            "startTimeMillis": 1413462240999,
            "durationMillis": 4999,
            "parentNodes": [
                "8"
            ]
        }
    ]
}
```

### GET /job/:`job-name`/:`run-id`/execution/node/:`node-id`/wfapi/log

Get the log for a Pipeline node.

Sample Response:

```json
{
    "nodeId": "6",
    "nodeStatus": "FAILED",
    "length": 295,
    "hasMore": false,
    "text": " > git rev-parse --is-inside-work-tree\nFetching changes from the remote Git repository\n > git config remote.origin.url https://github.com/tfennelly/simple-maven-project-with-test.git\nFetching upstream changes from https://github.com/tfennelly/simple-maven-project-with-test.git\n > git --version\n",
    "consoleUrl": "/jenkins/job/Build%20Github%20Repo/14/execution/node/6/log"
}
```

## Adding a REST Endpoint
This API currently implements REST endpoints via `TransientActionFactory` implementations.  Implementing a new endpoint
is very easy.  To help with the process, we have created a few helper classes:

1. [__`AbstractAPIActionHandler`__][AbstractAPIActionHandler]: An generic (not bound to any Pipeline model types) abstract base class that handles the request capture and response POJO
to JSON serialization (via [Jackson](https://github.com/FasterXML/jackson)).
1. [__`AbstractWorkflowJobActionHandler`__][AbstractWorkflowJobActionHandler]:  An abstract base class that extends `AbstractAPIActionHandler` to provide bindings to the `WorkflowJob` model
type.  Implementations of this type will be bound to URLs under `${rootURL}/job/<jobname>/`.

As an example, see the [`JobAPI`][JobAPI].  You'll see that its implementation is very simple:

* It's bound to `${rootURL}/job/<jobname>/wfapi` (see the `getUrlName()` method).
* It's `doDescribe()` method (bound to `${rootURL}/job/<jobname>/wfapi` and `${rootURL}/job/<jobname>/wfapi/describe`) implementation simply:
    1. Provides a description of the `WorkflowJob` to which it is bound (`target`).
    1. Specifies the `runsUrl` endpoint, which lists defaults of all the `WorkflowRun`s on the `WorkflowJob`.

Everything else is handled for [`JobAPI`][JobAPI] i.e. request capture, hooking to the
correct `WorkflowJob` model object instance, response (`WorkflowRunsExt`) serialization etc.

Implementing another endpoint to return some other `WorkflowJob` related dataset would be as simple as implementing another
[`AbstractWorkflowJobActionHandler`][AbstractWorkflowJobActionHandler], binding it to another URL under `${rootURL}/job/<jobname>/`.

If you need to implement an endpoint to return something other then `WorkflowJob` related data, it would probably be best to
first implement another base class along the same lines as [__`AbstractWorkflowJobActionHandler`__][AbstractWorkflowJobActionHandler].

[HAL]: https://tools.ietf.org/html/draft-kelly-json-hal-06
[AbstractAPIActionHandler]: src/main/java/com/cloudbees/workflow/rest/AbstractAPIActionHandler.java
[AbstractWorkflowJobActionHandler]: src/main/java/com/cloudbees/workflow/rest/AbstractWorkflowJobActionHandler.java
[JobAPI]: src/main/java/com/cloudbees/workflow/rest/endpoints/JobAPI.java
