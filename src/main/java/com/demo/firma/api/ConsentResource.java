package com.demo.firma.api;

import com.demo.firma.dto.ConsentRequest;
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

@Path("/api/signatures/{signatureId}/consent")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ConsentResource {

    @Inject
    SignatureService signatureService;

    @POST
    public Map<String, Object> registerConsent(
            @PathParam("signatureId") String signatureId,
            ConsentRequest request) {

        SignatureOperation operation =
                signatureService.registerConsent(signatureId, request);

        return SignatureResource.toResponse(operation);
    }
}
