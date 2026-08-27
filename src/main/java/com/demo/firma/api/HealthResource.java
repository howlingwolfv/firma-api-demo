package com.demo.firma.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@Path("/api/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    @GET
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "OK");
        response.put("service", "firma-api-demo");
        response.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        return response;
    }
}
