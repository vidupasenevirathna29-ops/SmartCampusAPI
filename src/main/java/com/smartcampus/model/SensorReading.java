package com.smartcampus.model;

/**
 * Stores a single reading from a sensor at a specific point in time.
 * We use this to keep a history of what values a sensor has recorded.
 */
public class SensorReading {

    /** A unique ID for this reading entry */
    private String id;

    /** When this reading was taken, stored as a Unix timestamp in milliseconds */
    private long timestamp;

    /** The measurement value from the sensor at the time of recording */
    private double value;

    // Constructors

    public SensorReading() {
    }

    public SensorReading(String id, long timestamp, double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
