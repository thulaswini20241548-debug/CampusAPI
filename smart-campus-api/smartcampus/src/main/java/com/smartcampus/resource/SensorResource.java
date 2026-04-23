package com.smartcampus.resource;
/**
 *
 * @author winil
 */

import com.smartcampus.application.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    // GET /api/v1/sensors — list all sensors, optional ?type= filter
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensors = new ArrayList<>(store.getSensors().values());
        if (type != null && !type.trim().isEmpty()) {
            sensors = sensors.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }
        return Response.ok(sensors).build();
    }

    // POST /api/v1/sensors — register new sensor, validates roomId exists
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Sensor ID is required."))
                    .build();
        }
        if (store.getSensors().containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody("Sensor '" + sensor.getId() + "' already exists."))
                    .build();
        }

        // Validate the referenced roomId actually exists
        if (sensor.getRoomId() == null || !store.getRooms().containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                "Cannot register sensor: Room '" + sensor.getRoomId() + "' does not exist in the system. " +
                "Please provide a valid roomId."
            );
        }

        // Default status if not provided
        if (sensor.getStatus() == null || sensor.getStatus().trim().isEmpty()) {
            sensor.setStatus("ACTIVE");
        }

        store.getSensors().put(sensor.getId(), sensor);
        store.getSensorReadings().put(sensor.getId(), new ArrayList<>());

        // Link sensor to its room
        Room room = store.getRooms().get(sensor.getRoomId());
        room.getSensorIds().add(sensor.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sensor registered successfully.");
        response.put("sensor", sensor);
        response.put("_links", linkMap(sensor.getId()));

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    // GET /api/v1/sensors/{sensorId} — get specific sensor
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor '" + sensorId + "' not found."))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    // PUT /api/v1/sensors/{sensorId} — update sensor status/value
    @PUT
    @Path("/{sensorId}")
    public Response updateSensor(@PathParam("sensorId") String sensorId, Sensor updated) {
        Sensor existing = store.getSensors().get(sensorId);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor '" + sensorId + "' not found."))
                    .build();
        }

        // Only allow updating status, type, and currentValue (not id or roomId)
        if (updated.getStatus() != null && !updated.getStatus().trim().isEmpty()) {
            String status = updated.getStatus().toUpperCase();
            if (!status.equals("ACTIVE") && !status.equals("MAINTENANCE") && !status.equals("OFFLINE")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorBody("Invalid status. Must be ACTIVE, MAINTENANCE, or OFFLINE."))
                        .build();
            }
            existing.setStatus(status);
        }
        if (updated.getType() != null && !updated.getType().trim().isEmpty()) {
            existing.setType(updated.getType());
        }
        if (updated.getCurrentValue() != 0) {
            existing.setCurrentValue(updated.getCurrentValue());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sensor '" + sensorId + "' updated successfully.");
        response.put("sensor", existing);
        response.put("_links", linkMap(sensorId));

        return Response.ok(response).build();
    }

    // DELETE /api/v1/sensors/{sensorId} — delete a sensor and unlink from room
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor '" + sensorId + "' not found."))
                    .build();
        }

        // Unlink sensor from its room
        Room room = store.getRooms().get(sensor.getRoomId());
        if (room != null) {
            room.getSensorIds().remove(sensorId);
        }

        // Remove sensor and its readings
        store.getSensors().remove(sensorId);
        store.getSensorReadings().remove(sensorId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sensor '" + sensorId + "' deleted and unlinked from room '" + sensor.getRoomId() + "' successfully.");
        return Response.ok(response).build();
    }

    // Sub-resource locator — delegates /sensors/{sensorId}/readings to SensorReadingResource
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor '" + sensorId + "' not found.");
        }
        return new SensorReadingResource(sensorId);
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", message);
        body.put("status", "error");
        return body;
    }

    private Map<String, String> linkMap(String sensorId) {
        Map<String, String> links = new HashMap<>();
        links.put("self", "/api/v1/sensors/" + sensorId);
        links.put("readings", "/api/v1/sensors/" + sensorId + "/readings");
        links.put("all-sensors", "/api/v1/sensors");
        return links;
    }
}