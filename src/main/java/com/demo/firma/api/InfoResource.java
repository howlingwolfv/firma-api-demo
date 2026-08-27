package com.demo.firma.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class InfoResource {

    @GET
    public Map<String, Object> info() {
        return Map.of(
                "service", "firma-api-demo",
                "version", "1.1.0",
                "fido2Mode", "WEBAUTHN_REAL",
                "endpoints", List.of(
                        "GET /api/health",
                        "POST /api/storage/test",
                        "POST /api/webauthn/register/options",
                        "POST /api/webauthn/register/verify",
                        "GET /api/webauthn/credentials/{username}",
                        "POST /api/signatures",
                        "GET /api/signatures/{id}",
                        "POST /api/signatures/{id}/document",
                        "GET /api/signatures/{id}/document",
                        "POST /api/signatures/{id}/consent",
                        "POST /api/signatures/{id}/biometric",
                        "POST /api/signatures/{id}/webauthn/options",
                        "POST /api/signatures/{id}/webauthn/verify",
                        "POST /api/signatures/{id}/finalize",
                        "GET /api/signatures/{id}/signed-document"
                )
        );
    }
}
