package com.demo.firma.adapter;

import com.demo.firma.dto.Fido2VerifyRequest;
import com.demo.firma.model.SignatureOperation;

public interface Fido2Adapter {
    VerificationResult verify(SignatureOperation operation, Fido2VerifyRequest request);
    record VerificationResult(boolean validated, String confirmationId, String credentialId, String mode) {}
}
