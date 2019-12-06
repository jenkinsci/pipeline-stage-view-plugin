# Changelog

## 2.12
August 6, 2019

-   Fix Internet Explorer 11
    compatibly [JENKINS-58808](https://issues.jenkins-ci.org/browse/JENKINS-58808)

#### 2.11 (April 19, 2019)

-   Avoid generating NaNy NaNd (in case of Nan)
-   Add chinese translation
-   Fix overlap when used with Test Result Trend  
-   Link to full node log from the window  
-   Improve usability of the download artefacts popup

#### 2.10 (Mar 30, 2018)

-   Fix a small bug with URL generation that could result in invalid URL
    generation in rare cases with customized Jenkins hostnames

#### 2.9 (Aug 29, 2017)

-   Fix a timing bug for run durations
    - [JENKINS-40162](https://issues.jenkins-ci.org/browse/JENKINS-40162)
-   Fix Jackson 2 dependencies by switching to use the Jenkins plugin
    rather than the
    library [JENKINS-46526](https://issues.jenkins-ci.org/browse/JENKINS-46526)
-   Fix an issue with Stage View caching where job renames generated bad
    links
    - [JENKINS-38159](https://issues.jenkins-ci.org/browse/JENKINS-38159)

#### 2.8 (May 25, 2017)

-   **Major feature**: Display arguments used in running steps in the
    logs view
    - [JENKINS-37324](https://issues.jenkins-ci.org/browse/JENKINS-37324)
-   Fix handling of BooleanParameter default to false
    - [JENKINS-36543](https://issues.jenkins-ci.org/browse/JENKINS-36543)

#### 2.6 (Mar 6, 2017)

-   [JENKINS-38536](https://issues.jenkins-ci.org/browse/JENKINS-38536)
    Fix timing of incomplete parallels

#### 2.5 (Feb 13, 2017)

-   Fix a minor CSS layout issue with stage view interfering with test
    results trends graphs

#### 2.4 (Nov 29, 2016)

-   Fix: Regression introduced in 2.3 which resulting in requesting all
    runs for a job, even if most were cached
    [JENKINS-40087](https://issues.jenkins-ci.org/browse/JENKINS-40087)

#### 2.3 (Nov 22, 2016)

-   Fix: Stage View showed multiple rows if displayName changed for a
    run (community PR, thank you!) -
    [JENKINS-34538](https://issues.jenkins-ci.org/browse/JENKINS-34538)

#### 2.2 (Nov 2, 2016)

-   CSS Fix: excess whitespace on sides of stages table with very high
    resolution monitors

#### 2.1 (Sept 30, 2016)

-   Fix: Prevent errors in deeply nested blocks from overflowing the
    stage status window
    ([JENKINS-37945](https://issues.jenkins-ci.org/browse/JENKINS-37945))
    -   Also filters out block start/end steps from that and log display
        (reverts to 1.7 behavior)
-   Fix: hide NotExecuted steps from log display (user is resuming from
    checkpoint, these steps never ran on this run)
-   CSS Fix: prevent excessively long error messages from overflowing
    stage status popup
-   Feature: display timing information for steps in log view
-   Don't show log-expand icons if there is no log message for a step
    ([JENKINS-31833](https://issues.jenkins-ci.org/browse/JENKINS-31833))

#### 2.0 (Aug 30, 2016)

-   Feature: support visualization of block-scoped stages (labelled
    blocks)
    ([JENKINS-26107](https://issues.jenkins-ci.org/browse/JENKINS-26107))
-   **Major feature**: complete rewrite of the flow analysis algorithms
    ([JENKINS-34038](https://issues.jenkins-ci.org/browse/JENKINS-34038))
    -   Needed to support block-scoped stage, and originally intended to
        allow for visualizing parallels
    -   Scalability: simpler and more efficient APIs allow for analyzing
        flows in a logical one-pass fashion.
    -   Performance: allows for far greater performance.  Currently
        roughly at parity with 1.7, but
        once [JENKINS-37569](https://issues.jenkins-ci.org/browse/JENKINS-37569) is
        done, should be \*much\* faster.
    -   Optimization: includes one final round of caching improvements.
         We can now serve requests about individual stages/nodes from
        the cache, and caches use less memory.

#### 1.7 (Aug 6, 2016)

-   Performance/bugfix: massively improve serverside run/stage caching,
    by greatly limiting excess cache invalidation
    -   Cache no longer fully invalidates on renaming a pipeline job:
        instead the old cache entries are removed and new ones created
        with the new job name
    -   Bugfix: Need to invalidate run cache results when their job is
        deleted
        ([JENKINS-36865](https://issues.jenkins-ci.org/browse/JENKINS-36865))
    -   Known issue: run cache can no longer serve requests for data
        about individual stages/nodes. 
        -   Mitigation: UI switches to use API below, so it just gets
            this data with the run request

-   Performance:  Fetch full stage data when requesting runs, and
    doesn't issue separate requests for individual stages
    ([JENKINS-34036](https://issues.jenkins-ci.org/browse/JENKINS-34036))
    -   API extension to allow getting full stage data for requested
        runs (or just the stage minimal data without child nodes)
    -   Front-end (browser) uses this API now
    -   It will still issue requests for in-progress builds though (room
        for some additional work)
    -   Clientside stage cache is redone to cache all completed stages
        when request issued.
-   Minor fix to CSS for unstable build
    ([JENKINS-37001](https://issues.jenkins-ci.org/browse/JENKINS-34036))

#### 1.6 (July 18, 2016)

Theme: Status handling and UX

-   Stage view correctly handles try/catch blocks
    ([JENKINS-34212](https://issues.jenkins-ci.org/browse/JENKINS-34212))
    -   Workaround: stages with errors are marked successful if overall
        build is.
-   Better support use of currentBuild.result to modify run status
    ([JENKINS-36523](https://issues.jenkins-ci.org/browse/JENKINS-36523) offers
    one example)
-   Support use of the UNSTABLE status for runs
    ([JENKINS-33700](https://issues.jenkins-ci.org/browse/JENKINS-33700))
    -   Catch: you can only set this status for the whole build (result
        is that successful stages will instead show as unstable). 
    -   Needs JENKINS-26522 to provide custom status tracking for a
        stage. 
-   Fix bizarre timing numbers from stage view
    ([JENKINS-34644](https://issues.jenkins-ci.org/browse/JENKINS-34644))
-   Fix Stage View jumping to the left when displaying many stages and
    scrolled to the right
    ([JENKINS-36384](https://issues.jenkins-ci.org/browse/JENKINS-36384))
-   Allow multi-line input prompts
    ([JENKINS-36661](https://issues.jenkins-ci.org/browse/JENKINS-36661))

#### 1.5 (June 21, 2016)

Theme: configurability of hardcoded values

-   REMOVE node labels in stage view
    (per [JENKINS-34038](https://issues.jenkins-ci.org/browse/JENKINS-34038))
    until we can correctly handle more of the cases with a block-based
    pipeline graph analysis
-   Add properties to override hardcoded limits in the REST API (i.e. to
    show more or less builds, artifacts, etc)
    ([JENKINS-34791](https://issues.jenkins-ci.org/browse/JENKINS-34791))
-   Add a property (modifiable at runtime) to disable lookup of Jenkins
    users for commit changelogs
    ([JENKINS-35484](https://issues.jenkins-ci.org/browse/JENKINS-35484)) 
    -   Useful for cases where user lookup ties to a security realm and
        caching problems or network calls render it expensive.

#### 1.4 (May 12, 2016)

-   Ensure the log view popup is always placed in the visible area of
    the page
     ([JENKINS-33109](https://issues.jenkins-ci.org/browse/JENKINS-33109))
-   Make full screen mode with use the full with by removing offsets for
    sidebar
    ( [JENKINS-33498](https://issues.jenkins-ci.org/browse/JENKINS-33498))
-   Fix display of artifact names when there are large numbers
    ([JENKINS-33351](https://issues.jenkins-ci.org/browse/JENKINS-33351))
-   Fix an NPE when generating links object with no active Stapler
    request
-   Correctly calculate all data sizes to use 1024 bytes per increment
    (consistency with other parts of Jenkins)
    ([JENKINS-33714](https://issues.jenkins-ci.org/browse/JENKINS-33714))

#### 1.3 (Apr 6, 2016)

-   Scalability enhancements dramatically improving UI performance with
    very large flow graphs
    ([JENKINS-33624](https://issues.jenkins-ci.org/browse/JENKINS-33624))
    -   Should greatly reduce CPU load on masters when viewing these
        graphs

#### 1.2 (Apr 6, 2016)

-   Fix issues introduced with Pipeline 2.0 that could produce linkage
    errors
-   Bump to parent POM v2.5

#### 1.1 (Apr 5, 2016)

-   Fix log details not expanding/collapsing for pipeline steps
    ([JENKINS-33110](https://issues.jenkins-ci.org/browse/JENKINS-33110))
-   Clean up POMs and use new parent POM
    ([JENKINS-33188](https://issues.jenkins-ci.org/browse/JENKINS-33188))
-   Fix a StackOverflow Exception when navigating long pipeline flow
    graphs
    ([JENKINS-33486](https://issues.jenkins-ci.org/browse/JENKINS-33486))

#### 1.0 (Feb 25, 2016)

-   Initial open source code release
