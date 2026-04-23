package com.smartcampus.mapper;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

/**
 * Handles the case where a sensor is being created with a roomId that doesn't exist.
 * We return 422 Unprocessable Entity because the request is valid JSON but the data doesn't make sense.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        return Response.status(422) // 422 means the request was understood but the linked resource wasn't found
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
