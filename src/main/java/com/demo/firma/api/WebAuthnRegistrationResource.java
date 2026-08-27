package com.demo.firma.api;

import com.demo.firma.dto.WebAuthnRegistrationOptionsRequest;
import com.demo.firma.dto.WebAuthnRegistrationVerifyRequest;
import com.demo.firma.webauthn.WebAuthnService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/webauthn")
@Produces(MediaType.APPLICATION_JSON)
public class WebAuthnRegistrationResource {

    @Inject
    WebAuthnService webAuthnService;

    @POST
    @Path("/register/options")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registrationOptions(
            WebAuthnRegistrationOptionsRequest request
    ) {
        String json = webAuthnService.startRegistration(
                request == null ? null : request.username(),
                request == null ? null : request.displayName()
        );

        return Response.ok(json, MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/register/verify")
    @Consumes(MediaType.APPLICATION_JSON)
    public WebAuthnService.RegistrationVerificationResult verifyRegistration(
            WebAuthnRegistrationVerifyRequest request
    ) {
        return webAuthnService.finishRegistration(
                request == null ? null : request.username(),
                request == null ? null : request.credential()
        );
    }

    @GET
    @Path("/credentials/{username}")
    public WebAuthnService.CredentialStatus credentialStatus(
            @PathParam("username") String username
    ) {
        return webAuthnService.credentialStatus(username);
    }
}
