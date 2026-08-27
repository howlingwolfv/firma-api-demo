package com.demo.firma.api;

import com.demo.firma.model.SignatureOperation;
import com.demo.firma.service.SignatureService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;

@Path("/api/signatures")
@Produces(MediaType.APPLICATION_JSON)
public class SignatureResource {

    @Inject
    SignatureService signatureService;

    @POST
    public Response createSignature() {
        SignatureOperation operation = signatureService.create();

        return Response.status(Response.Status.CREATED)
                .entity(toResponse(operation))
                .build();
    }

    @GET
    @Path("/{signatureId}")
    public Map<String, Object> getSignature(
            @PathParam("signatureId") String signatureId
    ) {
        return toResponse(signatureService.get(signatureId));
    }

    static Map<String, Object> toResponse(SignatureOperation operation) {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("signatureId", operation.getSignatureId());
        response.put("status", operation.getStatus().name());
        response.put("createdAt", operation.getCreatedAt());
        response.put("updatedAt", operation.getUpdatedAt());
        response.put("acceptedDocumentHash", operation.getAcceptedDocumentHash());
        response.put("consentAt", operation.getConsentAt());
        response.put("biometricTransactionId", operation.getBiometricTransactionId());
        response.put("biometricValidatedAt", operation.getBiometricValidatedAt());
        response.put("fido2Username", operation.getFido2Username());
        response.put("fido2CredentialId", operation.getFido2CredentialId());
        response.put("fido2ConfirmationId", operation.getFido2ConfirmationId());
        response.put("fido2ConfirmedAt", operation.getFido2ConfirmedAt());
        response.put("signedDocumentAvailable", operation.getSignedBlobName() != null);
        response.put("evidenceAvailable", operation.getEvidenceBlobName() != null);

        return response;
    }
}
