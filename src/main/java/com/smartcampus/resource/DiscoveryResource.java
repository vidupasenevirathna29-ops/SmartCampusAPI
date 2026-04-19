package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Discovery / Root endpoint — GET /api/v1/
 *
 * Returns API metadata including version, contact info, and a map of
 * available resource collections. This implements a basic form of HATEOAS
 * (Hypermedia As The Engine Of Application State) by providing navigation
 * links so clients can discover endpoints without relying solely on
 * external documentation.
 */
@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response discover() {

        // --- Links map (basic HATEOAS) ---
        Map<String, String> links = new HashMap<>();
        links.put("rooms",   "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");

        // --- Full response body ---
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
