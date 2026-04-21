package com.smartcampus.mapper;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

/**
 * Exception mapper for standard JAX-RS NotFoundException.
 * Maps to HTTP 404 Not Found.
 */
@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {
    @Override
    public Response toResponse(NotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of(
                        "error", "Not Found",
                        "message", "The requested resource or endpoint could not be found."
                ))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
