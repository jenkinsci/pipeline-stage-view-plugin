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

var restApi = require('./rest-api');

exports.getModelData = function (callback) {
    var objectUrl = this.requiredAttr('objectUrl');

    restApi.getObject(objectUrl, function (changesets) {

        var sumCommits = {
           commitCount:0,
           commits:[],
           contributorCount:0,
           contributors:[],
           kind:'multiple'
        };

        if (changesets.length > 0) {
            sumCommits.consoleUrl = changesets[0].consoleUrl;
        }

        // Go through each changeset and mine extra info
        for (var i = 0; i < changesets.length; i++) {
            var changeset = changesets[i];
            var commits = changeset.commits;
            var contributorName = {};

            changeset.contributors = [];
            for (var ii = 0; ii < commits.length; ii++) {
                var commit = commits[ii];

                // tag each changeset with the names of
                // the contributors to the commit list
                if (!contributorName[commit.authorJenkinsId]) {
                    contributorName[commit.authorJenkinsId] = 'yes';
                    changeset.contributors.push({
                        authorJenkinsId: commit.authorJenkinsId
                    });
                }

                // Parse out the first line of the commit message
                commit.messageLine1 = commit.message.split(/\r?\n/)[0];

                if (changeset.kind === 'git') {
                    commit.commitIdDisplay = commit.commitId.slice(0, 7);
                } else {
                    commit.commitIdDisplay = commit.commitId;
                }
                commit.kind = changeset.kind;
                sumCommits.commits.push(commit);
            }

            sumCommits.commitCount += changeset.commitCount;
            sumCommits.contributors = sumCommits.contributors.concat(changeset.contributors);
            sumCommits.contributorCount = sumCommits.contributors.length;
            if(changesets.length === 1)
                sumCommits.kind = changeset.kind;

            changeset.contributorCount = changeset.contributors.length;
        }


        return callback(sumCommits);
    });
}
