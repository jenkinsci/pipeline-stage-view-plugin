package com.cloudbees.workflow.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * POJO converted to JSON via Jackson as HTTP response.
 *
 * @author Kohsuke Kawaguchi
 */
public class JsonResponse implements HttpResponse {
    public final Object pojo;
    public final ObjectMapper mapper;

    public JsonResponse(ObjectMapper mapper, Object pojo) {
        this.mapper = mapper;
        this.pojo = pojo;
    }

    @Override
    public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
        rsp.setContentType("application/json; charset=UTF-8");
        mapper.writeValue(rsp.getOutputStream(),pojo);
    }
}
