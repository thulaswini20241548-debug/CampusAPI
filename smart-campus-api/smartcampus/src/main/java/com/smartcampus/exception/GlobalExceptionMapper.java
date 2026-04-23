package com.smartcampus.exception;
/**
 *
 * @author winil
 */


import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        // Log the full stack trace server-side for debugging, but NEVER expose it to the client
        LOGGER.log(Level.SEVERE, "Unexpected internal error: " + ex.getMessage(), ex);

        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("httpCode", 500);
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred. Please contact the system administrator.");

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
