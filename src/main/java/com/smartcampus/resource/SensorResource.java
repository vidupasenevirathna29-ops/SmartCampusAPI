package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JAX-RS resource managing the /api/v1/sensors collection.
 *
 * Endpoints:
 *   POST   /sensors                        → register a new sensor (validates roomId)
 *   GET    /sensors                        → list all sensors; optional ?type= filter
 *   GET    /sensors/{sensorId}             → get a single sensor (404 if missing)
 *   PUT    /sensors/{sensorId}             → update sensor fields
 *   DELETE /sensors/{sensorId}             → remove sensor, update parent room's sensorIds
 *
 * Sub-resource locator:
 *   /sensors/{sensorId}/readings           → delegates to SensorReadingResource
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    // =========================================================================
    // POST /sensors — register a new sensor
    // =========================================================================

    /**
     * Registers a new sensor.
     *
     * Validates that the supplied roomId exists.  If it does not,
     * throws LinkedResourceNotFoundException (mapper → 422 on Day 4).
     *
     * On success:
     *   - Sensor is stored in DataStore.sensors
     *   - Sensor ID is appended to the parent Room's sensorIds list
     *   - Returns 201 Created with Location header and the full sensor body
     *
     * Report Q3.1 — @Consumes:
     * Annotating with @Consumes(APPLICATION_JSON) tells Jersey to reject any
     * request whose Content-Type is not application/json with 415 Unsupported
     * Media Type before the method body even runs — preventing malformed data
     * from reaching business logic.
     */
    @POST
    public Response registerSensor(Sensor sensor, @Context UriInfo uriInfo) {
        if (sensor == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Request body must be a valid Sensor JSON object."))
                    .build();
        }

        // Validate parent room exists
        String roomId = sensor.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Field 'roomId' is required."))
                    .build();
        }

        Room parentRoom = store.getRooms().get(roomId);
        if (parentRoom == null) {
            // Throws → ExceptionMapper converts to 422 (Day 4)
            throw new LinkedResourceNotFoundException("Room", roomId);
        }

        // Auto-generate ID if absent
        if (sensor.getId() == null || sensor.getId().isBlank()) {
            sensor.setId(UUID.randomUUID().toString());
        }

        // Guard: ID conflict
        if (store.getSensors().containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "A sensor with id '" + sensor.getId() + "' already exists."))
                    .build();
        }

        // Default status if omitted
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        store.getSensors().put(sensor.getId(), sensor);

        // Side-effect: link sensor ID to parent room
        parentRoom.getSensorIds().add(sensor.getId());

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(sensor.getId())
                .build();

        return Response.created(location).entity(sensor).build();
    }

    // =========================================================================
    // GET /sensors — list all sensors, with optional ?type= filter
    // =========================================================================

    /**
     * Returns all sensors.  Optionally filter by sensor type using the
     * {@code ?type=} query parameter (case-insensitive).
     *
     * Example: GET /api/v1/sensors?type=CO2
     *
     * Report Q3.2 — @QueryParam vs path segment:
     * Filtering is an optional, optional narrowing of a collection — not a
     * distinct resource — so a query parameter (?type=CO2) is semantically
     * correct.  A path segment (/sensors/CO2) would imply CO2 is its own
     * addressable resource, which it is not.  Query params are also ignored
     * by caches that key on the base URI, giving better cache-ability.
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> result = new ArrayList<>(store.getSensors().values());

        if (type != null && !type.isBlank()) {
            result = result.stream()
                    .filter(s -> s.getType() != null
                            && s.getType().equalsIgnoreCase(type.trim()))
                    .collect(Collectors.toList());
        }

        return Response.ok(result).build();
    }

    // =========================================================================
    // GET /sensors/{sensorId} — get a single sensor
    // =========================================================================

    @GET
    @Path("{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Sensor not found.",
                            "sensorId", sensorId))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    // =========================================================================
    // PUT /sensors/{sensorId} — update sensor fields
    // =========================================================================

    /**
     * Updates mutable sensor fields: type, status, currentValue.
     * The ID and roomId are immutable after creation.
     */
    @PUT
    @Path("{sensorId}")
    public Response updateSensor(@PathParam("sensorId") String sensorId, Sensor updated) {
        Sensor existing = store.getSensors().get(sensorId);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Sensor not found.",
                            "sensorId", sensorId))
                    .build();
        }

        if (updated.getType() != null && !updated.getType().isBlank()) {
            existing.setType(updated.getType());
        }
        if (updated.getStatus() != null && !updated.getStatus().isBlank()) {
            existing.setStatus(updated.getStatus());
        }
        // currentValue 0.0 is a valid reading, so we always accept it when explicitly provided
        existing.setCurrentValue(updated.getCurrentValue());

        return Response.ok(existing).build();
    }

    // =========================================================================
    // DELETE /sensors/{sensorId} — remove sensor and unlink from room
    // =========================================================================

    /**
     * Deletes a sensor and removes its ID from the parent room's sensorIds list.
     * Returns 204 No Content on success.
     */
    @DELETE
    @Path("{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Sensor not found.",
                            "sensorId", sensorId))
                    .build();
        }

        // Unlink from parent room
        Room parentRoom = store.getRooms().get(sensor.getRoomId());
        if (parentRoom != null) {
            parentRoom.getSensorIds().remove(sensorId);
        }

        store.getSensors().remove(sensorId);
        // Also clean up any stored readings
        store.getSensorReadings().remove(sensorId);

        return Response.noContent().build();   // 204 No Content
    }

    // =========================================================================
    // Sub-Resource Locator — /sensors/{sensorId}/readings  (Day 3)
    // =========================================================================

    /**
     * Sub-resource locator for sensor readings history.
     * NO HTTP method annotation — Jersey calls this to resolve the sub-resource
     * class, then dispatches the actual HTTP method to SensorReadingResource.
     *
     * Report Q4.1 — Sub-Resource Locator benefits:
     * The locator pattern separates concerns cleanly: SensorReadingResource knows
     * nothing about routing; SensorResource knows nothing about reading business
     * logic. It also enables Jersey to lazily instantiate the sub-resource only
     * when that path is actually requested, and allows the sub-resource to receive
     * context (sensorId) via its constructor rather than through injection.
     */
    @Path("{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
