package com.smartcampus.mapper;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

/**
 * This handles the standard JAX-RS NotFoundException.
 * If someone hits an endpoint or resource that doesn't exist, we return a clean 404 JSON response
 * instead of letting Jersey throw its own default error.
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
