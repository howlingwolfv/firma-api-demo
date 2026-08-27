package com.demo.firma.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {
    @Override
    public Response toResponse(DomainException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", exception.getStatus());
        body.put("error", exception.getMessage());
        body.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        return Response.status(exception.getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
