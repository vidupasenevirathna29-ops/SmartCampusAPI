package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory data store for the Smart Campus API.
 *
 * SINGLETON PATTERN — only one instance exists for the entire application lifetime.
 *
 * WHY A SINGLETON?
 * JAX-RS resource classes are instantiated fresh for EVERY incoming HTTP request
 * (request-scoped by default). If we stored our maps inside resource classes,
 * all data would be lost after each request. By centralising state here in a
 * singleton, every resource instance reads from and writes to the same shared maps,
 * giving us persistent in-memory data across the lifetime of the server.
 *
 * THREAD SAFETY NOTE:
 * For this coursework, basic HashMap is used. In a production system you would
 * use ConcurrentHashMap and synchronised blocks (or java.util.concurrent locks)
 * to prevent race conditions under concurrent load.
 */
public class DataStore {

    // -------------------------------------------------------------------------
    // Singleton — eager initialisation (thread-safe, no lazy-init complexity)
    // -------------------------------------------------------------------------
    private static final DataStore INSTANCE = new DataStore();

    private DataStore() {
        seedData();
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------
    // In-memory collections
    // -------------------------------------------------------------------------

    /** Master map of all rooms. Key = room ID */
    private final Map<String, Room> rooms = new HashMap<>();

    /** Master map of all sensors. Key = sensor ID */
    private final Map<String, Sensor> sensors = new HashMap<>();

    /**
     * Historical readings per sensor.
     * Key = sensor ID, Value = ordered list of readings (newest last).
     */
    private final Map<String, List<SensorReading>> sensorReadings = new HashMap<>();

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    public Map<String, List<SensorReading>> getSensorReadings() {
        return sensorReadings;
    }

    // -------------------------------------------------------------------------
    // Seed data — pre-loaded examples for demo and Postman testing
    // -------------------------------------------------------------------------

    /**
     * Populates the store with realistic sample data so the API is immediately
     * testable after deployment without any manual POST requests.
     */
    private void seedData() {
        // --- Rooms ---
        Room lib301 = new Room("LIB-301", "Library Quiet Study", 50);
        Room lab202 = new Room("LAB-202", "Computer Science Lab", 30);
        Room hall1  = new Room("HALL-1",  "Main Lecture Hall",   200);

        rooms.put(lib301.getId(), lib301);
        rooms.put(lab202.getId(), lab202);
        rooms.put(hall1.getId(),  hall1);

        // --- Sensors ---
        Sensor temp001 = new Sensor("TEMP-001", "Temperature", "ACTIVE",   22.5, "LIB-301");
        Sensor co2001  = new Sensor("CO2-001",  "CO2",         "ACTIVE",  412.0, "LIB-301");
        Sensor occ001  = new Sensor("OCC-001",  "Occupancy",   "ACTIVE",   14.0, "LAB-202");
        Sensor temp002 = new Sensor("TEMP-002", "Temperature", "MAINTENANCE", 0.0, "LAB-202");

        sensors.put(temp001.getId(), temp001);
        sensors.put(co2001.getId(),  co2001);
        sensors.put(occ001.getId(),  occ001);
        sensors.put(temp002.getId(), temp002);

        // --- Link sensors to rooms ---
        lib301.getSensorIds().add("TEMP-001");
        lib301.getSensorIds().add("CO2-001");
        lab202.getSensorIds().add("OCC-001");
        lab202.getSensorIds().add("TEMP-002");

        // --- Seed some historical readings for TEMP-001 ---
        List<SensorReading> temp001Readings = new ArrayList<>();
        temp001Readings.add(new SensorReading("read-001", System.currentTimeMillis() - 3600000, 21.0));
        temp001Readings.add(new SensorReading("read-002", System.currentTimeMillis() - 1800000, 22.0));
        temp001Readings.add(new SensorReading("read-003", System.currentTimeMillis(),           22.5));
        sensorReadings.put("TEMP-001", temp001Readings);
    }
}
