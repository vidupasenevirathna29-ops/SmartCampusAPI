package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.*;

/**
 * This class handles reading history for a specific sensor.
 * It's used as a sub-resource from SensorResource, so it doesn't have its own @Path.
 * Jersey creates it through the locator in SensorResource.
 *
 * Supports:
 *   GET  /api/v1/sensors/{sensorId}/readings
 *   POST /api/v1/sensors/{sensorId}/readings
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store;

    /**
     * Called by the sub-resource locator in SensorResource.
     * Gets the sensorId from the URL so we know which sensor to work with.
     */
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
        this.store = DataStore.getInstance();
    }

    // GET /sensors/{sensorId}/readings — get all readings for this sensor

    @GET
    public Response getReadings() {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Sensor not found.",
                            "sensorId", sensorId))
                    .build();
        }

        List<SensorReading> readings = store.getSensorReadings()
                .getOrDefault(sensorId, Collections.emptyList());

        return Response.ok(readings).build();
    }

    // POST /sensors/{sensorId}/readings — add a new reading

    /**
     * Adds a new reading to the sensor's history.
     *
     * Rules we enforce:
     *   - The sensor must exist, otherwise 404
     *   - The sensor can't be in MAINTENANCE or OFFLINE status (returns 403)
     *   - We generate the ID and timestamp on the server side
     *   - We also update the sensor's currentValue with the new reading
     */
    @POST
    public Response addReading(SensorReading reading, @Context UriInfo uriInfo) {
        // Make sure the request body isn't empty
        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Request body must be a valid SensorReading JSON object."))
                    .build();
        }

        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Sensor not found.",
                            "sensorId", sensorId))
                    .build();
        }

        // Block the request if the sensor isn't active
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())
                || "OFFLINE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId, sensor.getStatus());
        }

        // Set the ID and timestamp here on the server
        reading.setId(UUID.randomUUID().toString());
        reading.setTimestamp(System.currentTimeMillis());

        // Update the sensor's current reading value
        sensor.setCurrentValue(reading.getValue());

        // Save the reading to the store
        store.getSensorReadings()
                .computeIfAbsent(sensorId, k -> new ArrayList<>())
                .add(reading);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(reading.getId())
                .build();

        return Response.created(location).entity(reading).build();
    }
}
