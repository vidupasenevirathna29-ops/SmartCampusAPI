package com.smartcampus.exception;

/**
 * This exception is thrown when a reading is submitted for a sensor that isn't active.
 * If the sensor is in MAINTENANCE or OFFLINE status, we block the request
 * because it wouldn't make sense to record data from an unavailable sensor.
 *
 * The ExceptionMapper handles this and returns a 403 Forbidden response.
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
