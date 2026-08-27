package com.demo.firma.api;

import com.demo.firma.dto.BiometricRequest;
import com.demo.firma.model.SignatureOperation;
import com.demo.firma.service.SignatureService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/api/signatures/{signatureId}/biometric")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BiometricResource {

    @Inject
    SignatureService signatureService;

    @POST
    public Map<String, Object> validateBiometric(
            @PathParam("signatureId") String signatureId,
            BiometricRequest request) {

        SignatureOperation operation =
                signatureService.validateBiometric(signatureId, request);

        return SignatureResource.toResponse(operation);
    }
}
