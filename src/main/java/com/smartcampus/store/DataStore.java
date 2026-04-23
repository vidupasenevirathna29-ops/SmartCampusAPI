package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the in-memory data store for the whole application.
 *
 * It uses the Singleton pattern so there's only ever one instance running.
 *
 * Why Singleton?
 * JAX-RS creates a new resource class instance for every HTTP request, so if we kept
 * our data inside the resource classes it would disappear after each request.
 * Putting everything here in a singleton means all requests share the same maps
 * and data stays alive as long as the server is running.
 *
 * Thread safety:
 * We're using basic HashMaps here which is fine for this coursework.
 * In a real production system you'd want ConcurrentHashMap to handle
 * multiple requests hitting the same data at the same time.
 */
public class DataStore {

    // Singleton setup — created once when the class loads
    private static final DataStore INSTANCE = new DataStore();

    private DataStore() {
        seedData();
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // The main in-memory collections

    /** Stores all rooms, keyed by room ID */
    private final Map<String, Room> rooms = new HashMap<>();

    /** Stores all sensors, keyed by sensor ID */
    private final Map<String, Sensor> sensors = new HashMap<>();

    /**
     * Stores reading history for each sensor.
     * Key = sensor ID, Value = list of readings in the order they were added.
     */
    private final Map<String, List<SensorReading>> sensorReadings = new HashMap<>();

    // Getters

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    public Map<String, List<SensorReading>> getSensorReadings() {
        return sensorReadings;
    }

    // Seed data — loaded on startup so the API is ready to test straight away

    /**
     * Loads some example rooms, sensors and readings when the app starts.
     * This means you can test all the endpoints straight away without having to POST any data first.
     */
    private void seedData() {
        // Create some sample rooms
        Room lib301 = new Room("LIB-301", "Library Quiet Study", 50);
        Room lab202 = new Room("LAB-202", "Computer Science Lab", 30);
        Room hall1  = new Room("HALL-1",  "Main Lecture Hall",   200);

        rooms.put(lib301.getId(), lib301);
        rooms.put(lab202.getId(), lab202);
        rooms.put(hall1.getId(),  hall1);

        // Create some sample sensors
        Sensor temp001 = new Sensor("TEMP-001", "Temperature", "ACTIVE",   22.5, "LIB-301");
        Sensor co2001  = new Sensor("CO2-001",  "CO2",         "ACTIVE",  412.0, "LIB-301");
        Sensor occ001  = new Sensor("OCC-001",  "Occupancy",   "ACTIVE",   14.0, "LAB-202");
        Sensor temp002 = new Sensor("TEMP-002", "Temperature", "MAINTENANCE", 0.0, "LAB-202");

        sensors.put(temp001.getId(), temp001);
        sensors.put(co2001.getId(),  co2001);
        sensors.put(occ001.getId(),  occ001);
        sensors.put(temp002.getId(), temp002);

        // Connect each sensor to its room
        lib301.getSensorIds().add("TEMP-001");
        lib301.getSensorIds().add("CO2-001");
        lab202.getSensorIds().add("OCC-001");
        lab202.getSensorIds().add("TEMP-002");

        // Add a few sample historical readings for TEMP-001
        List<SensorReading> temp001Readings = new ArrayList<>();
        temp001Readings.add(new SensorReading("read-001", System.currentTimeMillis() - 3600000, 21.0));
        temp001Readings.add(new SensorReading("read-002", System.currentTimeMillis() - 1800000, 22.0));
        temp001Readings.add(new SensorReading("read-003", System.currentTimeMillis(),           22.5));
        sensorReadings.put("TEMP-001", temp001Readings);
    }
}
