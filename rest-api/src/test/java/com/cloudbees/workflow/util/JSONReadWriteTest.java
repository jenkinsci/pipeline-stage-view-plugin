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
package com.cloudbees.workflow.util;

import com.cloudbees.workflow.rest.external.FlowNodeExt;
import com.cloudbees.workflow.rest.external.StatusExt;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class JSONReadWriteTest {

    @Test
    public void test_readWrite_string() throws Exception {
        JSONReadWrite readWrite = new JSONReadWrite();
        FlowNodeExt stageIn = new FlowNodeExt();

        stageIn.setName("Build");
        stageIn.setStatus(StatusExt.SUCCESS);
        stageIn.setStartTimeMillis(2222222);
        stageIn.setDurationMillis(111111);

        String json = readWrite.toString(stageIn);

        FlowNodeExt stageOut = readWrite.fromString(json, FlowNodeExt.class);

        Assert.assertEquals(stageIn.getName(), stageOut.getName());
        Assert.assertEquals(stageIn.getStatus(), stageOut.getStatus());
        Assert.assertEquals(stageIn.getStartTimeMillis(), stageOut.getStartTimeMillis());
        Assert.assertEquals(stageIn.getDurationMillis(), stageOut.getDurationMillis());
    }
}
