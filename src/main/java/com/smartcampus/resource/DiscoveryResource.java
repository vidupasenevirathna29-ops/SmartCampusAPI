package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the root endpoint of the API — GET /api/v1/
 *
 * It returns some basic info about the API like the version and contact details,
 * along with links to the main resources (rooms and sensors).
 * The _links section is a simple way to help clients find the available endpoints
 * without having to guess the URLs.
 */
@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response discover() {

        // Build the links map so clients know where to find rooms and sensors
        Map<String, String> links = new HashMap<>();
        links.put("rooms",   "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");

        // Put together the full response with API metadata and the links
        Map<String, Object> response = new HashMap<>();
        response.put("name",        "Smart Campus API");
        response.put("version",     "1.0");
        response.put("description", "RESTful API for managing campus rooms and environmental sensors.");
        response.put("contact",     "admin@smartcampus.ac.uk");
        response.put("status",      "operational");
        response.put("_links",      links);

        return Response.ok(response).build();
    }
}
