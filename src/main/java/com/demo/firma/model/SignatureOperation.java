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
    private String fido2Challenge;
    private OffsetDateTime fido2ChallengeExpiresAt;
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
    public String getFido2Challenge() { return fido2Challenge; }
    public void setFido2Challenge(String fido2Challenge) { this.fido2Challenge = fido2Challenge; }
    public OffsetDateTime getFido2ChallengeExpiresAt() { return fido2ChallengeExpiresAt; }
    public void setFido2ChallengeExpiresAt(OffsetDateTime fido2ChallengeExpiresAt) { this.fido2ChallengeExpiresAt = fido2ChallengeExpiresAt; }
    public String getFido2ConfirmationId() { return fido2ConfirmationId; }
    public void setFido2ConfirmationId(String fido2ConfirmationId) { this.fido2ConfirmationId = fido2ConfirmationId; }
    public OffsetDateTime getFido2ConfirmedAt() { return fido2ConfirmedAt; }
    public void setFido2ConfirmedAt(OffsetDateTime fido2ConfirmedAt) { this.fido2ConfirmedAt = fido2ConfirmedAt; }
}
