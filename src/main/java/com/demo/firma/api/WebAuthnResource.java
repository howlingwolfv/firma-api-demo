package com.demo.firma.api;

import com.demo.firma.dto.Fido2VerifyRequest;
import com.demo.firma.model.SignatureOperation;
import com.demo.firma.service.SignatureService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.LinkedHashMap;
import java.util.Map;

@Path("/api/signatures/{signatureId}/webauthn")
@Produces(MediaType.APPLICATION_JSON)
public class WebAuthnResource {

    @Inject
    SignatureService signatureService;

    @POST
    @Path("/challenge")
    public Map<String, Object> createChallenge(
            @PathParam("signatureId") String signatureId) {

        SignatureService.ChallengeResult result =
                signatureService.createFido2Challenge(signatureId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("challenge", result.challenge());
        response.put("expiresAt", result.expiresAt());
        response.put("mode", result.mode());
        return response;
    }

    @POST
    @Path("/verify")
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, Object> verify(
            @PathParam("signatureId") String signatureId,
            Fido2VerifyRequest request) {

        SignatureOperation operation =
                signatureService.verifyFido2(signatureId, request);

        return SignatureResource.toResponse(operation);
    }
}
