package com.demo.firma.service;

import com.demo.firma.adapter.BiometricAdapter;
import com.demo.firma.adapter.Fido2Adapter;
import com.demo.firma.dto.BiometricRequest;
import com.demo.firma.dto.ConsentRequest;
import com.demo.firma.dto.Fido2VerifyRequest;
import com.demo.firma.exception.DomainException;
import com.demo.firma.model.SignatureOperation;
import com.demo.firma.model.SignatureStatus;
import com.demo.firma.repository.SignatureRepository;
import com.demo.firma.storage.BlobStorageService;
import com.demo.firma.util.HashUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

@ApplicationScoped
public class SignatureService {

    private static final String DEFAULT_CONSENT_TEXT =
            "He leido el documento y acepto firmarlo electronicamente.";

    private final SecureRandom secureRandom = new SecureRandom();

    @Inject
    SignatureRepository repository;

    @Inject
    DocumentService documentService;

    @Inject
    BiometricAdapter biometricAdapter;

    @Inject
    Fido2Adapter fido2Adapter;

    @Inject
    BlobStorageService blobStorageService;

    @Inject
    PdfService pdfService;

    @Inject
    EvidenceService evidenceService;

    @ConfigProperty(name = "firma.fido2.challenge-ttl-seconds", defaultValue = "300")
    long challengeTtlSeconds;

    public SignatureOperation create() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        SignatureOperation operation = new SignatureOperation();
        operation.setSignatureId("SIG-" + shortId());
        operation.setStatus(SignatureStatus.CREATED);
        operation.setCreatedAt(now);
        operation.setUpdatedAt(now);

        repository.save(operation);
        return operation;
    }

    public SignatureOperation get(String signatureId) {
        return repository.findById(signatureId)
                .orElseThrow(() -> new DomainException(404, "Operacion de firma no encontrada"));
    }

    public SignatureOperation attachDocument(String signatureId, byte[] pdf) {
        SignatureOperation operation = get(signatureId);
        synchronized (operation) {
            documentService.attachOriginalPdf(operation, pdf);
            repository.save(operation);
            return operation;
        }
    }

    public byte[] getOriginalDocument(String signatureId) {
        return documentService.getOriginalPdf(get(signatureId));
    }

    public SignatureOperation registerConsent(String signatureId, ConsentRequest request) {
        SignatureOperation operation = get(signatureId);

        synchronized (operation) {
            requireStatus(operation, SignatureStatus.DOCUMENT_READY,
                    "El consentimiento requiere un documento listo");

            if (request == null || !request.accepted()) {
                throw new DomainException(400, "El consentimiento debe ser aceptado");
            }

            String consentText = request.consentText() == null || request.consentText().isBlank()
                    ? DEFAULT_CONSENT_TEXT
                    : request.consentText().trim();

            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            operation.setConsentTextHash(HashUtils.sha256(consentText));
            operation.setConsentAt(now);
            operation.setStatus(SignatureStatus.CONSENTED);
            operation.setUpdatedAt(now);

            repository.save(operation);
            return operation;
        }
    }

    public SignatureOperation validateBiometric(String signatureId, BiometricRequest request) {
        SignatureOperation operation = get(signatureId);

        synchronized (operation) {
            requireStatus(operation, SignatureStatus.CONSENTED,
                    "La biometria requiere consentimiento previo");

            BiometricAdapter.BiometricResult result = biometricAdapter.validate(request);
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            operation.setSignerName(result.signerName());
            operation.setBiometricTransactionId(result.transactionId());
            operation.setBiometricValidatedAt(now);
            operation.setStatus(SignatureStatus.BIOMETRIC_VALIDATED);
            operation.setUpdatedAt(now);

            repository.save(operation);
            return operation;
        }
    }

    public ChallengeResult createFido2Challenge(String signatureId) {
        SignatureOperation operation = get(signatureId);

        synchronized (operation) {
            requireStatus(operation, SignatureStatus.BIOMETRIC_VALIDATED,
                    "FIDO2 requiere biometria validada");

            byte[] nonce = new byte[32];
            secureRandom.nextBytes(nonce);

            String nonceBase64 = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(nonce);

            String canonicalContext =
                    operation.getSignatureId() + "|" +
                    operation.getAcceptedDocumentHash() + "|" +
                    operation.getConsentTextHash() + "|" +
                    operation.getBiometricTransactionId() + "|" +
                    nonceBase64;

            byte[] digest = hexToBytes(HashUtils.sha256(canonicalContext));

            String challenge = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(digest);

            OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC)
                    .plusSeconds(challengeTtlSeconds);

            operation.setFido2Challenge(challenge);
            operation.setFido2ChallengeExpiresAt(expiresAt);
            operation.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

            repository.save(operation);

            return new ChallengeResult(challenge, expiresAt, "DEMO");
        }
    }

    public SignatureOperation verifyFido2(String signatureId, Fido2VerifyRequest request) {
        SignatureOperation operation = get(signatureId);

        synchronized (operation) {
            requireStatus(operation, SignatureStatus.BIOMETRIC_VALIDATED,
                    "La confirmacion FIDO2 requiere biometria validada");

            if (operation.getFido2Challenge() == null ||
                    operation.getFido2ChallengeExpiresAt() == null) {
                throw new DomainException(409, "Primero debe generarse un challenge FIDO2");
            }

            if (OffsetDateTime.now(ZoneOffset.UTC)
                    .isAfter(operation.getFido2ChallengeExpiresAt())) {
                throw new DomainException(409, "El challenge FIDO2 ha expirado");
            }

            Fido2Adapter.VerificationResult result = fido2Adapter.verify(operation, request);
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            operation.setFido2ConfirmationId(result.confirmationId());
            operation.setFido2ConfirmedAt(now);
            operation.setStatus(SignatureStatus.FIDO2_CONFIRMED);
            operation.setUpdatedAt(now);

            repository.save(operation);
            return operation;
        }
    }

    public SignatureOperation finalizeSignature(String signatureId) {
        SignatureOperation operation = get(signatureId);

        synchronized (operation) {
            requireStatus(operation, SignatureStatus.FIDO2_CONFIRMED,
                    "La firma solo puede finalizarse despues de FIDO2");

            byte[] originalPdf = documentService.getOriginalPdf(operation);
            byte[] signedPdf = pdfService.appendElectronicSignatureCertificate(originalPdf, operation);

            String signedBlobName = "signed/" + operation.getSignatureId() + ".pdf";
            blobStorageService.upload(signedBlobName, signedPdf, true);

            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            operation.setSignedBlobName(signedBlobName);
            operation.setStatus(SignatureStatus.SIGNED);
            operation.setUpdatedAt(now);

            String evidenceBlobName = evidenceService.storeEvidence(operation);
            operation.setEvidenceBlobName(evidenceBlobName);

            repository.save(operation);
            return operation;
        }
    }

    public byte[] getSignedDocument(String signatureId) {
        return documentService.getSignedPdf(get(signatureId));
    }

    private void requireStatus(SignatureOperation operation,
                               SignatureStatus expected,
                               String message) {
        if (operation.getStatus() != expected) {
            throw new DomainException(409,
                    message + ". Estado actual: " + operation.getStatus());
        }
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private byte[] hexToBytes(String hex) {
        int length = hex.length();
        byte[] result = new byte[length / 2];

        for (int i = 0; i < length; i += 2) {
            result[i / 2] = (byte) (
                    (Character.digit(hex.charAt(i), 16) << 4) +
                    Character.digit(hex.charAt(i + 1), 16)
            );
        }
        return result;
    }

    public record ChallengeResult(String challenge, OffsetDateTime expiresAt, String mode) {}
}
