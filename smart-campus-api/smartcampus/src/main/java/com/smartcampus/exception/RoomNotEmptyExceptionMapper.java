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
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("httpCode", 409);
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        body.put("hint", "Remove or reassign all sensors in this room before deleting it.");

        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
