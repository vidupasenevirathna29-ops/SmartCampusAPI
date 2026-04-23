package com.smartcampus.model;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a room in the campus.
 * Each room can have multiple sensors attached to it that track things like temperature and humidity.
 */
public class Room {

    /** Unique ID for this room, like "LIB-301" */
    private String id;

    /** A display name for the room, e.g. "Library Quiet Study" */
    private String name;

    /** How many people the room can hold */
    private int capacity;

    /** List of sensor IDs that are installed in this room */
    private List<String> sensorIds = new ArrayList<>();

    // Constructors

    public Room() {
    }

    public Room(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public List<String> getSensorIds() {
        return sensorIds;
    }

    public void setSensorIds(List<String> sensorIds) {
        this.sensorIds = sensorIds;
    }
}
