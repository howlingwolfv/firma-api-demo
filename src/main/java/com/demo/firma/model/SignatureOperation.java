package com.demo.firma.model;

import java.time.OffsetDateTime;

public class SignatureOperation {

    private String signatureId;
    private SignatureStatus status;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private String originalBlobName;
    private String signedBlobName;
    private String evidenceBlobName;

    private String acceptedDocumentHash;

    private String consentTextHash;
    private OffsetDateTime consentAt;

    private String signerName;
    private String biometricTransactionId;
    private OffsetDateTime biometricValidatedAt;

    private String fido2Username;
    private String fido2CredentialId;
    private String fido2RequestHash;
    private String fido2ContextHash;
    private String fido2ConfirmationId;
    private OffsetDateTime fido2ConfirmedAt;

    public String getSignatureId() { return signatureId; }
    public void setSignatureId(String signatureId) { this.signatureId = signatureId; }

    public SignatureStatus getStatus() { return status; }
    public void setStatus(SignatureStatus status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getOriginalBlobName() { return originalBlobName; }
    public void setOriginalBlobName(String originalBlobName) { this.originalBlobName = originalBlobName; }

    public String getSignedBlobName() { return signedBlobName; }
    public void setSignedBlobName(String signedBlobName) { this.signedBlobName = signedBlobName; }

    public String getEvidenceBlobName() { return evidenceBlobName; }
    public void setEvidenceBlobName(String evidenceBlobName) { this.evidenceBlobName = evidenceBlobName; }

    public String getAcceptedDocumentHash() { return acceptedDocumentHash; }
    public void setAcceptedDocumentHash(String acceptedDocumentHash) { this.acceptedDocumentHash = acceptedDocumentHash; }

    public String getConsentTextHash() { return consentTextHash; }
    public void setConsentTextHash(String consentTextHash) { this.consentTextHash = consentTextHash; }

    public OffsetDateTime getConsentAt() { return consentAt; }
    public void setConsentAt(OffsetDateTime consentAt) { this.consentAt = consentAt; }

    public String getSignerName() { return signerName; }
    public void setSignerName(String signerName) { this.signerName = signerName; }

    public String getBiometricTransactionId() { return biometricTransactionId; }
    public void setBiometricTransactionId(String biometricTransactionId) { this.biometricTransactionId = biometricTransactionId; }

    public OffsetDateTime getBiometricValidatedAt() { return biometricValidatedAt; }
    public void setBiometricValidatedAt(OffsetDateTime biometricValidatedAt) { this.biometricValidatedAt = biometricValidatedAt; }

    public String getFido2Username() { return fido2Username; }
    public void setFido2Username(String fido2Username) { this.fido2Username = fido2Username; }

    public String getFido2CredentialId() { return fido2CredentialId; }
    public void setFido2CredentialId(String fido2CredentialId) { this.fido2CredentialId = fido2CredentialId; }

    public String getFido2RequestHash() { return fido2RequestHash; }
    public void setFido2RequestHash(String fido2RequestHash) { this.fido2RequestHash = fido2RequestHash; }

    public String getFido2ContextHash() { return fido2ContextHash; }
    public void setFido2ContextHash(String fido2ContextHash) { this.fido2ContextHash = fido2ContextHash; }

    public String getFido2ConfirmationId() { return fido2ConfirmationId; }
    public void setFido2ConfirmationId(String fido2ConfirmationId) { this.fido2ConfirmationId = fido2ConfirmationId; }

    public OffsetDateTime getFido2ConfirmedAt() { return fido2ConfirmedAt; }
    public void setFido2ConfirmedAt(OffsetDateTime fido2ConfirmedAt) { this.fido2ConfirmedAt = fido2ConfirmedAt; }
}
