package com.demo.firma.webauthn;

import com.demo.firma.exception.DomainException;
import com.demo.firma.model.SignatureOperation;
import com.demo.firma.util.HashUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class WebAuthnService {

    @Inject
    WebAuthnCredentialRepository credentialRepository;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "webauthn.rp.id")
    String rpId;

    @ConfigProperty(name = "webauthn.rp.name")
    String rpName;

    @ConfigProperty(name = "webauthn.rp.origin")
    String rpOrigin;

    @ConfigProperty(
            name = "webauthn.ceremony-ttl-seconds",
            defaultValue = "300"
    )
    long ceremonyTtlSeconds;

    private final Map<String, PendingRegistration> pendingRegistrations =
            new ConcurrentHashMap<>();

    private final Map<String, PendingAssertion> pendingAssertions =
            new ConcurrentHashMap<>();

    private RelyingParty relyingParty;

    @PostConstruct
    void init() {
        RelyingPartyIdentity identity = RelyingPartyIdentity.builder()
                .id(rpId)
                .name(rpName)
                .build();

        relyingParty = RelyingParty.builder()
                .identity(identity)
                .credentialRepository(credentialRepository)
                .origins(Set.of(rpOrigin))
                .build();
    }

    public String startRegistration(
            String username,
            String displayName
    ) {
        validateUsername(username);

        String effectiveDisplayName =
                displayName == null || displayName.isBlank()
                        ? username
                        : displayName.trim();

        UserIdentity user = credentialRepository.getOrCreateUser(
                username.trim(),
                effectiveDisplayName
        );

        AuthenticatorSelectionCriteria authenticatorSelection =
                AuthenticatorSelectionCriteria.builder()
                        .residentKey(ResidentKeyRequirement.REQUIRED)
                        .userVerification(UserVerificationRequirement.REQUIRED)
                        .build();

        PublicKeyCredentialCreationOptions request =
                relyingParty.startRegistration(
                        StartRegistrationOptions.builder()
                                .user(user)
                                .authenticatorSelection(authenticatorSelection)
                                .build()
                );

        OffsetDateTime expiresAt = now().plusSeconds(ceremonyTtlSeconds);

        pendingRegistrations.put(
                username.trim(),
                new PendingRegistration(request, user, expiresAt)
        );

        try {
            return request.toCredentialsCreateJson();
        } catch (Exception e) {
            throw new DomainException(
                    500,
                    "No fue posible serializar las opciones WebAuthn"
            );
        }
    }

    public RegistrationVerificationResult finishRegistration(
            String username,
            JsonNode credentialJson
    ) {
        validateUsername(username);

        if (credentialJson == null || credentialJson.isNull()) {
            throw new DomainException(400, "Debe enviarse la credencial WebAuthn");
        }

        PendingRegistration pending = pendingRegistrations.get(username.trim());

        if (pending == null) {
            throw new DomainException(
                    409,
                    "No existe un registro WebAuthn pendiente para el usuario"
            );
        }

        ensureNotExpired(pending.expiresAt(), "El registro WebAuthn ha expirado");

        try {
            String credentialString = objectMapper.writeValueAsString(credentialJson);

            PublicKeyCredential<
                    AuthenticatorAttestationResponse,
                    ClientRegistrationExtensionOutputs
                    > credential = PublicKeyCredential
                    .parseRegistrationResponseJson(credentialString);

            RegistrationResult result = relyingParty.finishRegistration(
                    FinishRegistrationOptions.builder()
                            .request(pending.request())
                            .response(credential)
                            .build()
            );

            if (!result.isUserVerified()) {
                throw new DomainException(
                        401,
                        "El autenticador no realizó verificación de usuario"
                );
            }

            RegisteredCredential registeredCredential =
                    RegisteredCredential.builder()
                            .credentialId(result.getKeyId().getId())
                            .userHandle(pending.user().getId())
                            .publicKeyCose(result.getPublicKeyCose())
                            .signatureCount(result.getSignatureCount())
                            .build();

            credentialRepository.addCredential(
                    username.trim(),
                    result.getKeyId(),
                    registeredCredential
            );

            pendingRegistrations.remove(username.trim());

            return new RegistrationVerificationResult(
                    username.trim(),
                    result.getKeyId().getId().getBase64Url(),
                    result.isUserVerified(),
                    result.isDiscoverable().orElse(false),
                    credentialRepository.credentialCount(username.trim())
            );

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainException(
                    400,
                    "La respuesta de registro WebAuthn no pudo validarse: " +
                            e.getMessage()
            );
        }
    }

    public String startSignatureAssertion(
            String signatureId,
            String username,
            SignatureOperation operation
    ) {
        validateUsername(username);

        String normalizedUsername = username.trim();

        if (!credentialRepository.hasCredentials(normalizedUsername)) {
            throw new DomainException(
                    409,
                    "El usuario no tiene una passkey registrada"
            );
        }

        String contextHash = signatureContextHash(operation);

        AssertionRequest request = relyingParty.startAssertion(
                StartAssertionOptions.builder()
                        .username(normalizedUsername)
                        .userVerification(UserVerificationRequirement.REQUIRED)
                        .build()
        );

        OffsetDateTime expiresAt = now().plusSeconds(ceremonyTtlSeconds);

        try {
            String requestHash = HashUtils.sha256(request.toJson());

            pendingAssertions.put(
                    signatureId,
                    new PendingAssertion(
                            normalizedUsername,
                            request,
                            requestHash,
                            contextHash,
                            expiresAt
                    )
            );

            return request.toCredentialsGetJson();
        } catch (Exception e) {
            throw new DomainException(
                    500,
                    "No fue posible generar las opciones de autenticación WebAuthn"
            );
        }
    }

    public AssertionVerificationResult finishSignatureAssertion(
            String signatureId,
            JsonNode credentialJson,
            SignatureOperation operation
    ) {
        if (credentialJson == null || credentialJson.isNull()) {
            throw new DomainException(400, "Debe enviarse la assertion WebAuthn");
        }

        PendingAssertion pending = pendingAssertions.get(signatureId);

        if (pending == null) {
            throw new DomainException(
                    409,
                    "No existe una autenticación WebAuthn pendiente para esta firma"
            );
        }

        ensureNotExpired(
                pending.expiresAt(),
                "La autenticación WebAuthn ha expirado"
        );

        String currentContextHash = signatureContextHash(operation);

        if (!pending.contextHash().equals(currentContextHash)) {
            pendingAssertions.remove(signatureId);
            throw new DomainException(
                    409,
                    "El contexto de la operación cambió después de generar el challenge"
            );
        }

        try {
            String credentialString = objectMapper.writeValueAsString(credentialJson);

            PublicKeyCredential<
                    AuthenticatorAssertionResponse,
                    ClientAssertionExtensionOutputs
                    > credential = PublicKeyCredential
                    .parseAssertionResponseJson(credentialString);

            AssertionResult result = relyingParty.finishAssertion(
                    FinishAssertionOptions.builder()
                            .request(pending.request())
                            .response(credential)
                            .build()
            );

            if (!result.isSuccess()) {
                throw new DomainException(401, "La assertion WebAuthn no fue válida");
            }

            if (!result.isUserVerified()) {
                throw new DomainException(
                        401,
                        "El autenticador no realizó verificación de usuario"
                );
            }

            if (!pending.username().equals(result.getUsername())) {
                throw new DomainException(
                        401,
                        "La credencial WebAuthn no pertenece al usuario esperado"
                );
            }

            credentialRepository.updateSignatureCount(
                    result.getCredential().getCredentialId(),
                    result.getSignatureCount()
            );

            pendingAssertions.remove(signatureId);

            return new AssertionVerificationResult(
                    result.getUsername(),
                    result.getCredential().getCredentialId().getBase64Url(),
                    result.isUserVerified(),
                    pending.requestHash(),
                    pending.contextHash()
            );

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainException(
                    400,
                    "La assertion WebAuthn no pudo validarse: " + e.getMessage()
            );
        }
    }

    public CredentialStatus credentialStatus(String username) {
        validateUsername(username);

        return new CredentialStatus(
                username.trim(),
                credentialRepository.credentialCount(username.trim())
        );
    }

    private String signatureContextHash(SignatureOperation operation) {
        String canonicalContext = String.join(
                "|",
                safe(operation.getSignatureId()),
                safe(operation.getAcceptedDocumentHash()),
                safe(operation.getConsentTextHash()),
                safe(operation.getBiometricTransactionId())
        );

        return HashUtils.sha256(canonicalContext);
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new DomainException(400, "username es obligatorio");
        }
    }

    private void ensureNotExpired(
            OffsetDateTime expiresAt,
            String message
    ) {
        if (now().isAfter(expiresAt)) {
            throw new DomainException(409, message);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record PendingRegistration(
            PublicKeyCredentialCreationOptions request,
            UserIdentity user,
            OffsetDateTime expiresAt
    ) {
    }

    private record PendingAssertion(
            String username,
            AssertionRequest request,
            String requestHash,
            String contextHash,
            OffsetDateTime expiresAt
    ) {
    }

    public record RegistrationVerificationResult(
            String username,
            String credentialId,
            boolean userVerified,
            boolean discoverable,
            int credentialCount
    ) {
    }

    public record AssertionVerificationResult(
            String username,
            String credentialId,
            boolean userVerified,
            String requestHash,
            String contextHash
    ) {
    }

    public record CredentialStatus(
            String username,
            int credentialCount
    ) {
    }
}
