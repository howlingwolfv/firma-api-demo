package com.demo.firma.service;

import com.demo.firma.model.SignatureOperation;
import com.demo.firma.storage.BlobStorageService;
import com.demo.firma.util.HashUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class EvidenceService {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    BlobStorageService blobStorageService;

    public String storeEvidence(SignatureOperation operation) {
        try {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("signatureId", operation.getSignatureId());
            evidence.put("status", operation.getStatus().name());
            evidence.put("createdAt", operation.getCreatedAt());

            Map<String, Object> document = new LinkedHashMap<>();
            document.put("acceptedDocumentHash", operation.getAcceptedDocumentHash());
            document.put("originalBlob", operation.getOriginalBlobName());
            document.put("signedBlob", operation.getSignedBlobName());
            evidence.put("document", document);

            Map<String, Object> consent = new LinkedHashMap<>();
            consent.put("consentTextHash", operation.getConsentTextHash());
            consent.put("acceptedAt", operation.getConsentAt());
            evidence.put("consent", consent);

            Map<String, Object> biometric = new LinkedHashMap<>();
            biometric.put("transactionId", operation.getBiometricTransactionId());
            biometric.put("validatedAt", operation.getBiometricValidatedAt());
            biometric.put("mode", "DEMO");
            evidence.put("biometric", biometric);

            Map<String, Object> fido2 = new LinkedHashMap<>();
            fido2.put("challengeHash", operation.getFido2Challenge() == null
                    ? null
                    : HashUtils.sha256(operation.getFido2Challenge()));
            fido2.put("confirmationId", operation.getFido2ConfirmationId());
            fido2.put("confirmedAt", operation.getFido2ConfirmedAt());
            fido2.put("mode", "DEMO");
            evidence.put("fido2", fido2);

            byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(evidence);

            String blobName = "evidence/" + operation.getSignatureId() + ".json";
            blobStorageService.upload(blobName, json, true);
            return blobName;

        } catch (Exception e) {
            throw new IllegalStateException("No fue posible guardar el expediente de evidencias", e);
        }
    }
}
