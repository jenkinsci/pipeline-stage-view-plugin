/*
 * The MIT License
 *
 * Copyright (c) 2015, CloudBees, Inc.
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
package com.cloudbees.workflow.rest.external;

import com.cloudbees.workflow.util.JSONReadWrite;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URL;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class ChangeSetExtTest {
    
    @Test
    @Issue("CJP-3040")
    public void test_getCommitUrl_hasChangeSetLink() throws IOException {
        RepositoryBrowser repositoryBrowser = Mockito.mock(RepositoryBrowser.class);
        ChangeLogSet.Entry entry = Mockito.mock(ChangeLogSet.Entry.class);
        URL aUrl = new URL("http://blah");
        
        Mockito.when(repositoryBrowser.getChangeSetLink(Mockito.any(ChangeLogSet.Entry.class))).thenReturn(aUrl);
        String urlAsString = ChangeSetExt.getCommitUrl(repositoryBrowser, entry);
        Assert.assertEquals(aUrl.toString(), urlAsString);
    }
    
    @Test
    @Issue("CJP-3040")
    public void test_getCommitUrl_hasntChangeSetLink() throws IOException {
        RepositoryBrowser repositoryBrowser = Mockito.mock(RepositoryBrowser.class);
        ChangeLogSet.Entry entry = Mockito.mock(ChangeLogSet.Entry.class);
        
        Mockito.when(repositoryBrowser.getChangeSetLink(Mockito.any(ChangeLogSet.Entry.class))).thenReturn(null);
        String urlAsString = ChangeSetExt.getCommitUrl(repositoryBrowser, entry);
        Assert.assertNull(urlAsString);
    }
    
    @Test
    @Issue("CJP-3040")
    public void test_getCommitUrl_noRepBrowser() throws IOException {
        ChangeLogSet.Entry entry = Mockito.mock(ChangeLogSet.Entry.class);

        String urlAsString = ChangeSetExt.getCommitUrl(null, entry);
        Assert.assertNull(urlAsString);
    }
    
    @Test
    public void test_Commit_objSerialization_hasCommitUrl() throws IOException {
        ChangeSetExt.Commit commit = new ChangeSetExt.Commit();
        
        commit.setCommitId("fc9f243");
        commit.setMessage("Make it good");
        commit.setAuthorJenkinsId("tfennelly");
        commit.setTimestamp(123123123L);
        commit.setCommitUrl("http://blah");

        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        String asString = jsonReadWrite.toString(commit);
        JSONObject asJSONObject = JSONObject.fromObject(asString);
        Assert.assertEquals("http://blah", asJSONObject.getString("commitUrl"));
    }
    
    @Test
    public void test_Commit_objSerialization_hasntCommitUrl() throws IOException {
        ChangeSetExt.Commit commit = new ChangeSetExt.Commit();
        
        commit.setCommitId("fc9f243");
        commit.setMessage("Make it good");
        commit.setAuthorJenkinsId("tfennelly");
        commit.setTimestamp(123123123L);
        commit.setCommitUrl(null);

        JSONReadWrite jsonReadWrite = new JSONReadWrite();
        String asString = jsonReadWrite.toString(commit);
        JSONObject asJSONObject = JSONObject.fromObject(asString);
        Assert.assertFalse(asJSONObject.has("commitUrl"));
    }
}
