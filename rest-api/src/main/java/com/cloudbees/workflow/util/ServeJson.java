package com.cloudbees.workflow.util;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.Interceptor;
import org.kohsuke.stapler.interceptor.InterceptorAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * Put this on stapler action methods (i.e. {@code doXyz(...)}) to render the returned POJO object
 * as JSON.
 *
 * @author Kohsuke Kawaguchi
 */
@Retention(RUNTIME)
@Target(METHOD)
@InterceptorAnnotation(ServeJson.Processor.class)
public @interface ServeJson {
    public static class Processor extends Interceptor {
        @Override
        public Object invoke(StaplerRequest request, StaplerResponse response, Object instance, Object[] arguments)
                throws IllegalAccessException, InvocationTargetException {
            try {
                Object o = target.invoke(request, response, instance, arguments);
                return new JsonResponse(JSONReadWrite.jsonMapper,o);
            } catch (Exception e) {
                // TODO: Can be removed and ServletException added to throws declarations from 1.651+
                throw new RuntimeException("Unexpected exception while serving JSON", e);
            }
        }
    }
}
