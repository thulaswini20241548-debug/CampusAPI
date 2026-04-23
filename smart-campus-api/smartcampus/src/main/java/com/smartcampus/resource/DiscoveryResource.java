package com.smartcampus.resource;
/**
 *
 * @author winil
 */

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response discover() {
        Map<String, Object> info = new HashMap<>();
        info.put("api", "Smart Campus Sensor & Room Management API");
        info.put("version", "1.0.0");
        info.put("description", "RESTful API for managing campus rooms and IoT sensors");
        info.put("contact", "admin@smartcampus.ac.uk");
        info.put("status", "operational");

        Map<String, String> links = new HashMap<>();
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        info.put("resources", links);

        Map<String, String> hateoas = new HashMap<>();
        hateoas.put("self", "/api/v1");
        hateoas.put("rooms", "/api/v1/rooms");
        hateoas.put("sensors", "/api/v1/sensors");
        info.put("_links", hateoas);

        return Response.ok(info).build();
    }
}
