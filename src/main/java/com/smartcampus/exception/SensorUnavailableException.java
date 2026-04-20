package com.smartcampus.exception;

/**
 * Thrown when a POST /sensors/{id}/readings is attempted but the
 * target sensor is currently in MAINTENANCE (or OFFLINE) status.
 *
 * The ExceptionMapper (Day 4) will convert this into HTTP 403 Forbidden.
 */
public class SensorUnavailableException extends RuntimeException {

    private final String sensorId;
    private final String currentStatus;

    public SensorUnavailableException(String sensorId, String currentStatus) {
        super("Sensor '" + sensorId + "' cannot accept readings while in status: " + currentStatus);
        this.sensorId = sensorId;
        this.currentStatus = currentStatus;
    }

    public String getSensorId() {
        return sensorId;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }
}
