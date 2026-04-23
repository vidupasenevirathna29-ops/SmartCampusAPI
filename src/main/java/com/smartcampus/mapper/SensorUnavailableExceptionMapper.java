package com.smartcampus.mapper;

import com.smartcampus.exception.SensorUnavailableException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

/**
 * This mapper handles the SensorUnavailableException.
 * If someone tries to post a reading to a sensor that's in MAINTENANCE or OFFLINE,
 * we block it here and return a 403 Forbidden with details about the sensor's current status.
 */
@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {
    @Override
    public Response toResponse(SensorUnavailableException exception) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(Map.of(
                        "error", "Forbidden",
                        "message", exception.getMessage(),
                        "sensorId", exception.getSensorId(),
                        "currentStatus", exception.getCurrentStatus()
                ))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
