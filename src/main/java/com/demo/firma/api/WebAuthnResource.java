package com.demo.firma.api;

import com.demo.firma.dto.WebAuthnAssertionOptionsRequest;
import com.demo.firma.dto.WebAuthnAssertionVerifyRequest;
import com.demo.firma.model.SignatureOperation;
import com.demo.firma.service.SignatureService;
import com.demo.firma.webauthn.WebAuthnService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/api/signatures/{signatureId}/webauthn")
@Produces(MediaType.APPLICATION_JSON)
public class WebAuthnResource {

    @Inject
    SignatureService signatureService;

    @Inject
    WebAuthnService webAuthnService;

    @POST
    @Path("/options")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response assertionOptions(
            @PathParam("signatureId") String signatureId,
            WebAuthnAssertionOptionsRequest request
    ) {
        SignatureOperation operation =
                signatureService.getReadyForFido2(signatureId);

        String json = webAuthnService.startSignatureAssertion(
                signatureId,
                request == null ? null : request.username(),
                operation
        );

        return Response.ok(json, MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/verify")
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, Object> verify(
            @PathParam("signatureId") String signatureId,
            WebAuthnAssertionVerifyRequest request
    ) {
        SignatureOperation operation =
                signatureService.getReadyForFido2(signatureId);

        WebAuthnService.AssertionVerificationResult result =
                webAuthnService.finishSignatureAssertion(
                        signatureId,
                        request == null ? null : request.credential(),
                        operation
                );

        SignatureOperation confirmed = signatureService.confirmRealFido2(
                signatureId,
                result.username(),
                result.credentialId(),
                result.requestHash(),
                result.contextHash()
        );

        return SignatureResource.toResponse(confirmed);
    }
}
