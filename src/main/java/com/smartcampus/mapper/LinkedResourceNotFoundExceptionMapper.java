package com.smartcampus.mapper;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

/**
 * Exception mapper for LinkedResourceNotFoundException.
 * Maps to HTTP 422 Unprocessable Entity.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        return Response.status(422) // 422 Unprocessable Entity
                .entity(Map.of(
                        "error", "Unprocessable Entity",
                        "message", exception.getMessage(),
                        "resourceType", exception.getResourceType(),
                        "resourceId", exception.getResourceId()
                ))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
