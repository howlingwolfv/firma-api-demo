package com.demo.firma.adapter;

import com.demo.firma.dto.Fido2VerifyRequest;
import com.demo.firma.exception.DomainException;
import com.demo.firma.model.SignatureOperation;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class DemoFido2Adapter implements Fido2Adapter {
    @Override
    public VerificationResult verify(SignatureOperation operation, Fido2VerifyRequest request) {
        if (request == null || !request.approved()) {
            throw new DomainException(400, "La confirmación FIDO2 demo debe ser aprobada");
        }
        String credentialId = request.credentialId() == null || request.credentialId().isBlank()
                ? "demo-credential"
                : request.credentialId().trim();
        return new VerificationResult(true, "FIDO-DEMO-" + shortId(), credentialId, "DEMO");
    }
    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
