package com.smartcampus.model;

/**
 * This class represents a sensor that is installed in a campus room.
 * Each sensor has a type, a current value, and a status to show if it's working or not.
 */
public class Sensor {

    /** Unique ID for the sensor, e.g. "TEMP-001" */
    private String id;

    /** What the sensor measures — e.g. "Temperature", "CO2", "Occupancy", "Lighting" */
    private String type;

    /**
     * Whether the sensor is currently working or not.
     * Can be "ACTIVE", "MAINTENANCE", or "OFFLINE".
     */
    private String status;

    /** The last reading value recorded by this sensor */
    private double currentValue;

    /** The ID of the room this sensor belongs to */
    private String roomId;

    // Constructors

    public Sensor() {
    }

    public Sensor(String id, String type, String status, double currentValue, String roomId) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.currentValue = currentValue;
        this.roomId = roomId;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
