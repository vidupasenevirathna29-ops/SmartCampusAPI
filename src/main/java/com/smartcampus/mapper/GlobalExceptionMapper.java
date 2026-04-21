package com.smartcampus.mapper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global exception mapper to catch any unhandled Throwables.
 * Maps to HTTP 500 Internal Server Error, returning a safe generic message
 * and preventing stack traces from leaking to the client.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    
    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        // Log the actual exception securely on the server side — never expose the stack trace to the client
        LOGGER.log(Level.SEVERE, "Unhandled server exception: " + exception.getMessage(), exception);

        // Return a generic safe message to the client
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of(
                        "error", "Internal Server Error",
                        "message", "An unexpected error occurred. Please try again later."
                ))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
