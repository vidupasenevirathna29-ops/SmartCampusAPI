package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.*;

/**
 * Handles all the CRUD operations for rooms at /api/v1/rooms.
 *
 * Endpoints:
 *   GET    /rooms              → list all rooms
 *   POST   /rooms              → create a new room (returns 201 + Location header)
 *   GET    /rooms/{roomId}     → get a single room by ID (404 if not found)
 *   PUT    /rooms/{roomId}     → update an existing room (404 if not found)
 *   DELETE /rooms/{roomId}     → delete a room, but blocked with 409 if it still has sensors
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    // GET /rooms — return all rooms

    /**
     * Returns a list of all rooms in the system.
     *
     * We return full Room objects including their sensor IDs so the client
     * doesn't need to make extra calls just to see what's in each room.
     * It's a bit more data but it saves a lot of round-trips.
     */
    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(store.getRooms().values());
        return Response.ok(roomList).build();
    }

    // POST /rooms — create a new room

    /**
     * Creates a new room and saves it to the store.
     * If no ID is given in the request body, we generate one automatically.
     * Returns 201 Created with a Location header pointing to the new room.
     */
    @POST
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        if (room == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Request body must be a valid Room JSON object."))
                    .build();
        }

        // If no ID was sent, generate one
        if (room.getId() == null || room.getId().isBlank()) {
            room.setId(UUID.randomUUID().toString());
        }

        // If a room with this ID already exists, reject with 409
        if (store.getRooms().containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "A room with id '" + room.getId() + "' already exists."))
                    .build();
        }

        // Make sure the sensorIds list isn't null
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }

        store.getRooms().put(room.getId(), room);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(room.getId())
                .build();

        return Response.created(location).entity(room).build();
    }

    // GET /rooms/{roomId} — fetch one room by ID

    /**
     * Looks up a room by its ID and returns it, or 404 if it doesn't exist.
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

    // PUT /rooms/{roomId} — update a room

    /**
     * Updates the name and/or capacity of an existing room.
     * We use the ID from the URL path — any ID in the request body is ignored.
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

    // DELETE /rooms/{roomId} — remove a room

    /**
     * Deletes a room by ID.
     *
     * If the room still has sensors linked to it, we throw a RoomNotEmptyException
     * which the ExceptionMapper converts to a 409 Conflict.
     *
     * We return 404 if the room doesn't exist, so the client knows it's already gone.
     * The server state is the same either way — no room — so this is still idempotent.
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

        // If the room still has sensors, block the delete
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId);
        }

        store.getRooms().remove(roomId);
        return Response.noContent().build(); // 204 No Content — successful delete
    }
}
