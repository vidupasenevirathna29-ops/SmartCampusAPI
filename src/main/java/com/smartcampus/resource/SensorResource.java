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
 * Handles all CRUD operations for sensors at /api/v1/sensors.
 *
 * Endpoints:
 *   POST   /sensors                        → register a new sensor (checks that roomId exists)
 *   GET    /sensors                        → list all sensors; supports optional ?type= filter
 *   GET    /sensors/{sensorId}             → get one sensor by ID (404 if not found)
 *   PUT    /sensors/{sensorId}             → update sensor fields
 *   DELETE /sensors/{sensorId}             → remove sensor and unlink it from the parent room
 *
 * Sub-resource locator:
 *   /sensors/{sensorId}/readings           → delegates to SensorReadingResource
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    // POST /sensors — register a new sensor

    /**
     * Registers a new sensor in the system.
     *
     * We check that the roomId actually points to a real room before saving.
     * If the room doesn't exist, we throw LinkedResourceNotFoundException which
     * gets converted to a 422 response.
     *
     * On success:
     *   - Sensor is saved to the DataStore
     *   - Sensor ID is added to the parent room's sensorIds list
     *   - Returns 201 Created with a Location header
     *
     * Using @Consumes(APPLICATION_JSON) means Jersey automatically rejects
     * requests with the wrong Content-Type before we even see them.
     */
    @POST
    public Response registerSensor(Sensor sensor, @Context UriInfo uriInfo) {
        if (sensor == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Request body must be a valid Sensor JSON object."))
                    .build();
        }

        // Make sure a roomId was provided
        String roomId = sensor.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Field 'roomId' is required."))
                    .build();
        }

        Room parentRoom = store.getRooms().get(roomId);
        if (parentRoom == null) {
            // Room doesn't exist — ExceptionMapper will turn this into 422
            throw new LinkedResourceNotFoundException("Room", roomId);
        }

        // Generate an ID if the client didn't provide one
        if (sensor.getId() == null || sensor.getId().isBlank()) {
            sensor.setId(UUID.randomUUID().toString());
        }

        // Reject if another sensor already has this ID
        if (store.getSensors().containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "A sensor with id '" + sensor.getId() + "' already exists."))
                    .build();
        }

        // If no status was given, default it to ACTIVE
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        store.getSensors().put(sensor.getId(), sensor);

        // Also add this sensor's ID to the room so the room knows about it
        parentRoom.getSensorIds().add(sensor.getId());

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(sensor.getId())
                .build();

        return Response.created(location).entity(sensor).build();
    }

    // GET /sensors — list all sensors with optional ?type= filter

    /**
     * Returns all sensors, or a filtered list if a ?type= query param is given.
     *
     * Example: GET /api/v1/sensors?type=CO2
     *
     * We use a query param instead of a path segment because filtering is optional —
     * it narrows down the collection rather than identifying a separate resource.
     * Something like /sensors/CO2 would imply CO2 is its own resource, which it isn't.
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

    // GET /sensors/{sensorId} — fetch a single sensor by ID

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

    // PUT /sensors/{sensorId} — update a sensor

    /**
     * Updates one or more fields on an existing sensor (type, status, currentValue).
     * The sensor ID and roomId can't be changed after creation.
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
        // 0.0 is a valid reading value, so we always update currentValue
        existing.setCurrentValue(updated.getCurrentValue());

        return Response.ok(existing).build();
    }

    // DELETE /sensors/{sensorId} — remove a sensor and clean up

    /**
     * Deletes a sensor and removes it from the parent room's sensor list.
     * Also clears any stored readings for this sensor.
     * Returns 204 No Content if successful.
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

        // Remove this sensor from the room's list
        Room parentRoom = store.getRooms().get(sensor.getRoomId());
        if (parentRoom != null) {
            parentRoom.getSensorIds().remove(sensorId);
        }

        store.getSensors().remove(sensorId);
        // Clean up any readings stored for this sensor too
        store.getSensorReadings().remove(sensorId);

        return Response.noContent().build(); // 204 No Content
    }

    // Sub-Resource Locator — /sensors/{sensorId}/readings

    /**
     * This locator method hands off requests to SensorReadingResource.
     * It doesn't have an HTTP method annotation — Jersey uses it to figure out
     * which class should handle the /readings path, then routes the actual request there.
     *
     * This keeps the reading logic separate from the sensor routing logic,
     * and lets us pass the sensorId directly into the sub-resource via its constructor.
     */
    @Path("{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
