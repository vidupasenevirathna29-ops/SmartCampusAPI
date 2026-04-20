package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.*;

/**
 * JAX-RS resource managing the /api/v1/rooms collection.
 *
 * Endpoints:
 *   GET    /rooms              → list all rooms
 *   POST   /rooms              → create a new room (201 + Location header)
 *   GET    /rooms/{roomId}     → get a single room (404 if missing)
 *   PUT    /rooms/{roomId}     → update an existing room (404 if missing)
 *   DELETE /rooms/{roomId}     → delete room; guard: 409 if sensors still present
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    // =========================================================================
    // GET /rooms — return all rooms as a JSON list
    // =========================================================================

    /**
     * Returns the complete list of rooms.
     *
     * Report Q2.1: We return the full Room objects (including their sensorIds list)
     * rather than just IDs.  The trade-off is a slightly larger payload, but it saves
     * clients from making N extra round-trips to resolve each room — a classic
     * "over-fetching vs chattiness" decision. For a campus dashboard that always
     * renders the full room list, returning full objects is the right choice.
     */
    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(store.getRooms().values());
        return Response.ok(roomList).build();
    }

    // =========================================================================
    // POST /rooms — create a new room
    // =========================================================================

    /**
     * Creates a new room.
     * If the request body omits an ID, a UUID is auto-generated.
     * Returns 201 Created with a Location header pointing to the new resource.
     */
    @POST
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        if (room == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Request body must be a valid Room JSON object."))
                    .build();
        }

        // Auto-generate ID if the client did not supply one
        if (room.getId() == null || room.getId().isBlank()) {
            room.setId(UUID.randomUUID().toString());
        }

        // Guard: ID conflict
        if (store.getRooms().containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "A room with id '" + room.getId() + "' already exists."))
                    .build();
        }

        // Ensure sensorIds list is initialised
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }

        store.getRooms().put(room.getId(), room);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(room.getId())
                .build();

        return Response.created(location).entity(room).build();
    }

    // =========================================================================
    // GET /rooms/{roomId} — get a single room
    // =========================================================================

    /**
     * Returns a single room by ID, or 404 if it does not exist.
     */
    @GET
    @Path("{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Room not found.",
                            "roomId", roomId))
                    .build();
        }
        return Response.ok(room).build();
    }

    // =========================================================================
    // PUT /rooms/{roomId} — update an existing room
    // =========================================================================

    /**
     * Replaces an existing room's mutable fields (name, capacity).
     * The ID in the path is authoritative; any ID supplied in the body is ignored.
     */
    @PUT
    @Path("{roomId}")
    public Response updateRoom(@PathParam("roomId") String roomId, Room updated) {
        Room existing = store.getRooms().get(roomId);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Room not found.",
                            "roomId", roomId))
                    .build();
        }

        if (updated.getName() != null && !updated.getName().isBlank()) {
            existing.setName(updated.getName());
        }
        if (updated.getCapacity() > 0) {
            existing.setCapacity(updated.getCapacity());
        }

        return Response.ok(existing).build();
    }

    // =========================================================================
    // DELETE /rooms/{roomId} — delete a room (guarded)
    // =========================================================================

    /**
     * Deletes a room identified by roomId.
     *
     * Guard: if the room still has sensors, throws RoomNotEmptyException.
     * The ExceptionMapper (Day 4) will turn that into HTTP 409 Conflict.
     *
     * Report Q2.2 — DELETE idempotency:
     * REST DELETE should be idempotent: calling it multiple times has the same
     * server-side effect as calling it once. We return 404 on a missing room
     * (rather than 204) so clients know the resource was already gone — this
     * is acceptable per RFC 9110; the state of the server is still "room absent".
     */
    @DELETE
    @Path("{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Room not found.",
                            "roomId", roomId))
                    .build();
        }

        // Guard: room still has sensors → 409 (mapper wired on Day 4)
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId);
        }

        store.getRooms().remove(roomId);
        return Response.noContent().build();   // 204 No Content
    }
}
