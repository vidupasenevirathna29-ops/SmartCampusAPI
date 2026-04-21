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
 * Sub-resource for sensor reading history.
 * Accessed via the sub-resource locator in SensorResource:
 *   GET  /api/v1/sensors/{sensorId}/readings
 *   POST /api/v1/sensors/{sensorId}/readings
 *
 * NOT registered as a root resource — no @Path annotation at class level.
 * Jersey instantiates this class through the locator in SensorResource.
 *
 * Day 3 implementation complete.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store;

    /**
     * Constructor called by the sub-resource locator in SensorResource.
     * Receives the sensorId from the URL path.
     */
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
        this.store = DataStore.getInstance();
    }

    // =========================================================================
    // GET /sensors/{sensorId}/readings — return all readings for this sensor
    // =========================================================================

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

    // =========================================================================
    // POST /sensors/{sensorId}/readings — append a new reading
    // =========================================================================

    /**
     * Appends a new reading to the sensor's history.
     *
     * Business rules (enforced on Day 3):
     *   - Sensor must exist (404)
     *   - Sensor status must NOT be "MAINTENANCE" (throws SensorUnavailableException → 403)
     *   - ID is auto-generated (UUID)
     *   - Timestamp is set server-side to System.currentTimeMillis()
     *   - Side effect: sensor.currentValue is updated to the new reading's value
     *
     * Day 3 implementation complete.
     */
    @POST
    public Response addReading(SensorReading reading, @Context UriInfo uriInfo) {
        // Validate request body first
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

        // Guard: sensor under maintenance or offline cannot accept readings
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())
                || "OFFLINE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId, sensor.getStatus());
        }

        // Server-side metadata
        reading.setId(UUID.randomUUID().toString());
        reading.setTimestamp(System.currentTimeMillis());

        // Side effect: update the sensor's current value
        sensor.setCurrentValue(reading.getValue());

        // Persist the reading
        store.getSensorReadings()
                .computeIfAbsent(sensorId, k -> new ArrayList<>())
                .add(reading);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(reading.getId())
                .build();

        return Response.created(location).entity(reading).build();
    }
}
