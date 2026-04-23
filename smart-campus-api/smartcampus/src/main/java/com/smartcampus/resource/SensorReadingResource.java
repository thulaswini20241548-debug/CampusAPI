package com.smartcampus.resource;
/**
 *
 * @author winil
 */

import com.smartcampus.application.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * Sub-resource class for /api/v1/sensors/{sensorId}/readings
 * Instantiated by SensorResource via the sub-resource locator pattern.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // GET /api/v1/sensors/{sensorId}/readings — fetch reading history
    @GET
    public Response getReadings() {
        List<SensorReading> readings = store.getSensorReadings()
                .getOrDefault(sensorId, new ArrayList<>());

        Map<String, Object> response = new HashMap<>();
        response.put("sensorId", sensorId);
        response.put("totalReadings", readings.size());
        response.put("readings", readings);
        return Response.ok(response).build();
    }

    // POST /api/v1/sensors/{sensorId}/readings — append a new reading
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensors().get(sensorId);

        // Part 5.3: Block readings for sensors in MAINTENANCE status
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                "Sensor '" + sensorId + "' is currently under MAINTENANCE and cannot accept new readings. " +
                "Please wait until the sensor is back ACTIVE."
            );
        }

        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Reading body is required."))
                    .build();
        }

        // Assign ID and timestamp if not provided
        if (reading.getId() == null || reading.getId().trim().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Store the reading
        store.getSensorReadings()
             .computeIfAbsent(sensorId, k -> new ArrayList<>())
             .add(reading);

        // Side effect: update the parent sensor's currentValue
        sensor.setCurrentValue(reading.getValue());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Reading recorded successfully.");
        response.put("reading", reading);
        response.put("updatedSensorValue", sensor.getCurrentValue());
        response.put("_links", linkMap());

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", message);
        body.put("status", "error");
        return body;
    }

    private Map<String, String> linkMap() {
        Map<String, String> links = new HashMap<>();
        links.put("self", "/api/v1/sensors/" + sensorId + "/readings");
        links.put("sensor", "/api/v1/sensors/" + sensorId);
        return links;
    }
}
