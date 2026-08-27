package com.demo.firma.service;

import com.demo.firma.adapter.BiometricAdapter;
import com.demo.firma.dto.BiometricRequest;
import com.demo.firma.dto.ConsentRequest;
import com.demo.firma.exception.DomainException;
import com.demo.firma.model.SignatureOperation;
import com.demo.firma.model.SignatureStatus;
import com.demo.firma.repository.SignatureRepository;
import com.demo.firma.storage.BlobStorageService;
import com.demo.firma.util.HashUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@ApplicationScoped
public class SignatureService {

    private static final String DEFAULT_CONSENT_TEXT =
            "He leído el documento y acepto firmarlo electrónicamente.";

    @Inject
    SignatureRepository repository;

    @Inject
    DocumentService documentService;

    @Inject
    BiometricAdapter biometricAdapter;

    @Inject
    BlobStorageService blobStorageService;

    @Inject
    PdfService pdfService;

    @Inject
    EvidenceService evidenceService;

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
                .orElseThrow(() -> new DomainException(
                        404,
                        "Operación de firma no encontrada"
                ));
    }

    public SignatureOperation attachDocument(
            String signatureId,
            byte[] pdf
    ) {
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

    public SignatureOperation registerConsent(
            String signatureId,
            ConsentRequest request
    ) {
        SignatureOperation operation = get(signatureId);

        synchronized (operation) {
            requireStatus(
                    operation,
                    SignatureStatus.DOCUMENT_READY,
                    "El consentimiento requiere un documento listo"
            );

            if (request == null || !request.accepted()) {
                throw new DomainException(
                        400,
                        "El consentimiento debe ser aceptado"
                );
            }

            String consentText =
                    request.consentText() == null || request.consentText().isBlank()
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

    public SignatureOperation validateBiometric(
            String signatureId,
            BiometricRequest request
    ) {
        SignatureOperation operation = get(signatureId);

        synchronized (operation) {
            requireStatus(
                    operation,
                    SignatureStatus.CONSENTED,
                    "La biometría requiere consentimiento previo"
            );

            BiometricAdapter.BiometricResult result =
                    biometricAdapter.validate(request);

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

    public SignatureOperation getReadyForFido2(String signatureId) {
        SignatureOperation operation = get(signatureId);

        requireStatus(
                operation,
                SignatureStatus.BIOMETRIC_VALIDATED,
                "WebAuthn requiere biometría validada"
        );

        return operation;
    }

    public SignatureOperation confirmRealFido2(
            String signatureId,
            String username,
            String credentialId,
            String requestHash,
            String contextHash
    ) {
        SignatureOperation operation = get(signatureId);

        synchronized (operation) {
            requireStatus(
                    operation,
                    SignatureStatus.BIOMETRIC_VALIDATED,
                    "La confirmación WebAuthn requiere biometría validada"
            );

            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            operation.setFido2Username(username);
            operation.setFido2CredentialId(credentialId);
            operation.setFido2RequestHash(requestHash);
            operation.setFido2ContextHash(contextHash);
            operation.setFido2ConfirmationId("WEBAUTHN-" + shortId());
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
            requireStatus(
                    operation,
                    SignatureStatus.FIDO2_CONFIRMED,
                    "La firma solo puede finalizarse después de WebAuthn/FIDO2"
            );

            byte[] originalPdf = documentService.getOriginalPdf(operation);

            byte[] signedPdf =
                    pdfService.appendElectronicSignatureCertificate(
                            originalPdf,
                            operation
                    );

            String signedBlobName =
                    "signed/" + operation.getSignatureId() + ".pdf";

            blobStorageService.upload(
                    signedBlobName,
                    signedPdf,
                    true
            );

            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            operation.setSignedBlobName(signedBlobName);
            operation.setStatus(SignatureStatus.SIGNED);
            operation.setUpdatedAt(now);

            String evidenceBlobName =
                    evidenceService.storeEvidence(operation);

            operation.setEvidenceBlobName(evidenceBlobName);
            repository.save(operation);

            return operation;
        }
    }

    public byte[] getSignedDocument(String signatureId) {
        return documentService.getSignedPdf(get(signatureId));
    }

    private void requireStatus(
            SignatureOperation operation,
            SignatureStatus expected,
            String message
    ) {
        if (operation.getStatus() != expected) {
            throw new DomainException(
                    409,
                    message + ". Estado actual: " + operation.getStatus()
            );
        }
    }

    private String shortId() {
        return UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }
}
