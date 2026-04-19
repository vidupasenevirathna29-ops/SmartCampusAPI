package com.smartcampus.model;

/**
 * Represents a sensor deployed within a campus room.
 * Sensors record real-time measurements (temperature, CO2, occupancy, etc.).
 */
public class Sensor {

    /** Unique identifier, e.g. "TEMP-001" */
    private String id;

    /** Category of sensor: "Temperature", "CO2", "Occupancy", "Lighting" */
    private String type;

    /**
     * Current operational state.
     * Valid values: "ACTIVE", "MAINTENANCE", "OFFLINE"
     */
    private String status;

    /** The most recent measurement recorded by this sensor */
    private double currentValue;

    /** Foreign key — links this sensor to its parent Room */
    private String roomId;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Sensor() {
    }

    public Sensor(String id, String type, String status, double currentValue, String roomId) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.currentValue = currentValue;
        this.roomId = roomId;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

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
