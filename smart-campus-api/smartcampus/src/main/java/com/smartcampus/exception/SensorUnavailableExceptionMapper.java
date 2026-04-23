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

@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("httpCode", 403);
        body.put("error", "Forbidden");
        body.put("message", ex.getMessage());
        body.put("hint", "Change sensor status to ACTIVE before submitting readings.");

        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
