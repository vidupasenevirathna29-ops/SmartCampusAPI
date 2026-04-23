package com.smartcampus.mapper;

import com.smartcampus.exception.RoomNotEmptyException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

/**
 * This mapper handles the RoomNotEmptyException.
 * When someone tries to delete a room that still has sensors, we catch it here
 * and return a 409 Conflict to tell the client they need to remove the sensors first.
 */
@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {
    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(Map.of(
                        "error", "Conflict",
                        "message", exception.getMessage()
                ))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
