package com.demo.firma.api;

import com.demo.firma.model.SignatureOperation;
import com.demo.firma.service.SignatureService;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/api/signatures/{signatureId}/finalize")
@Produces(MediaType.APPLICATION_JSON)
public class FinalizationResource {

    @Inject
    SignatureService signatureService;

    @POST
    public Map<String, Object> finalizeSignature(
            @PathParam("signatureId") String signatureId) {

        SignatureOperation operation =
                signatureService.finalizeSignature(signatureId);

        return SignatureResource.toResponse(operation);
    }
}
