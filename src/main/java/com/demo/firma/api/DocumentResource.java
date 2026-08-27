package com.demo.firma.api;

import com.demo.firma.model.SignatureOperation;
import com.demo.firma.service.SignatureService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/api/signatures/{signatureId}")
public class DocumentResource {

    @Inject
    SignatureService signatureService;

    @POST
    @Path("/document")
    @Consumes("application/pdf")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> uploadDocument(
            @PathParam("signatureId") String signatureId,
            byte[] pdf) {

        SignatureOperation operation =
                signatureService.attachDocument(signatureId, pdf);

        return SignatureResource.toResponse(operation);
    }

    @GET
    @Path("/document")
    @Produces("application/pdf")
    public Response getDocument(
            @PathParam("signatureId") String signatureId) {

        byte[] pdf = signatureService.getOriginalDocument(signatureId);

        return Response.ok(pdf)
                .type("application/pdf")
                .header("Content-Disposition",
                        "inline; filename=" + signatureId + "-original.pdf")
                .build();
    }

    @GET
    @Path("/signed-document")
    @Produces("application/pdf")
    public Response getSignedDocument(
            @PathParam("signatureId") String signatureId) {

        byte[] pdf = signatureService.getSignedDocument(signatureId);

        return Response.ok(pdf)
                .type("application/pdf")
                .header("Content-Disposition",
                        "inline; filename=" + signatureId + "-signed.pdf")
                .build();
    }
}
