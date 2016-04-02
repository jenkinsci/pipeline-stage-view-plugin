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

import hudson.model.Item;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class ModelUtil {

    private ModelUtil() {
    }

    public static String getFullItemUrl(FlowNode item) {
        try {
            return getFullItemUrl(item.getUrl());
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected error getting URL for a Stage FlowNode.", e);
        }
    }

    public static String getFullItemUrl(String itemUrl) {
        String rootUrl = ModelUtil.getRootUrl();
        if (!itemUrl.endsWith("/")) {
            itemUrl += "/";
        }
        return rootUrl + "/" + itemUrl;
    }

    public static String getRootUrl() {
        StaplerRequest currentRequest = Stapler.getCurrentRequest();
        return (currentRequest != null) ? currentRequest.getContextPath() : "/";
    }
}
