package com.smartcampus.mapper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a catch-all exception mapper for anything we didn't handle specifically.
 * If something unexpected goes wrong, we don't want to expose the stack trace to the client,
 * so we log it on the server and just return a generic 500 error message.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    
    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        // Log the full error on the server so we can debug it, but don't send it to the client
        LOGGER.log(Level.SEVERE, "Unhandled server exception: " + exception.getMessage(), exception);

        // Just send a plain 500 response with a safe message
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of(
                        "error", "Internal Server Error",
                        "message", "An unexpected error occurred. Please try again later."
                ))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
