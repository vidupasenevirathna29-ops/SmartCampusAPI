package com.smartcampus;

import com.smartcampus.filter.ApiLoggingFilter;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS Application entry point.
 *
 * @ApplicationPath sets the versioned base path for all API resources.
 * Combined with ROOT.war deployment on Tomcat, all endpoints are
 * accessible at: http://localhost:8080/api/v1/...
 *
 * Lifecycle note: By default, JAX-RS creates a NEW instance of each
 * resource class per HTTP request (request-scoped). This is why shared
 * state (rooms, sensors, readings) lives in the DataStore singleton,
 * NOT as instance fields inside resource classes.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // --- Resources ---
        classes.add(DiscoveryResource.class);
        classes.add(RoomResource.class);
        classes.add(SensorResource.class);

        // --- JSON Support (Jackson via Jersey media module) ---
        classes.add(JacksonFeature.class);

        // --- Filters (logging) ---
        classes.add(ApiLoggingFilter.class);

        // --- Exception Mappers ---
        classes.add(com.smartcampus.mapper.RoomNotEmptyExceptionMapper.class);
        classes.add(com.smartcampus.mapper.LinkedResourceNotFoundExceptionMapper.class);
        classes.add(com.smartcampus.mapper.SensorUnavailableExceptionMapper.class);
        classes.add(com.smartcampus.mapper.NotFoundExceptionMapper.class);
        classes.add(com.smartcampus.mapper.GlobalExceptionMapper.class);

        return classes;
    }
}
