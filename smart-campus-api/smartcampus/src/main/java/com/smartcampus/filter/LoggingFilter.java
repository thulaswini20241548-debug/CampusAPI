package com.smartcampus.filter;
/**
 *
 * @author winil
 */

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String msg = "[SMART-CAMPUS][REQUEST]  " + new Date()
                + " | Method: " + requestContext.getMethod()
                + " | URI: " + requestContext.getUriInfo().getRequestUri();
        LOGGER.log(Level.INFO, msg);
        System.out.println(msg);
        System.out.flush();
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        String msg = "[SMART-CAMPUS][RESPONSE] " + new Date()
                + " | Method: " + requestContext.getMethod()
                + " | URI: " + requestContext.getUriInfo().getRequestUri()
                + " | Status: " + responseContext.getStatus()
                + " " + responseContext.getStatusInfo().getReasonPhrase();
        LOGGER.log(Level.INFO, msg);
        System.out.println(msg);
        System.out.flush();
    }
}