package com.demo.firma.webauthn;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.UserIdentity;
import jakarta.enterprise.context.ApplicationScoped;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class WebAuthnCredentialRepository implements CredentialRepository {

    private final SecureRandom secureRandom = new SecureRandom();

    private final Map<String, UserIdentity> usersByUsername =
            new ConcurrentHashMap<>();

    private final Map<ByteArray, String> usernamesByHandle =
            new ConcurrentHashMap<>();

    private final Map<ByteArray, StoredCredential> credentialsById =
            new ConcurrentHashMap<>();

    public UserIdentity getOrCreateUser(
            String username,
            String displayName
    ) {
        return usersByUsername.computeIfAbsent(username, key -> {
            byte[] userHandleBytes = new byte[32];
            secureRandom.nextBytes(userHandleBytes);

            UserIdentity user = UserIdentity.builder()
                    .name(username)
                    .displayName(displayName)
                    .id(new ByteArray(userHandleBytes))
                    .build();

            usernamesByHandle.put(user.getId(), username);
            return user;
        });
    }

    public boolean hasCredentials(String username) {
        return credentialsById.values().stream()
                .anyMatch(value -> value.username().equals(username));
    }

    public int credentialCount(String username) {
        return (int) credentialsById.values().stream()
                .filter(value -> value.username().equals(username))
                .count();
    }

    public void addCredential(
            String username,
            PublicKeyCredentialDescriptor descriptor,
            RegisteredCredential credential
    ) {
        credentialsById.put(
                credential.getCredentialId(),
                new StoredCredential(username, descriptor, credential)
        );
    }

    public void updateSignatureCount(
            ByteArray credentialId,
            long signatureCount
    ) {
        credentialsById.computeIfPresent(credentialId, (key, stored) -> {
            RegisteredCredential updated = stored.credential()
                    .toBuilder()
                    .signatureCount(signatureCount)
                    .build();

            return new StoredCredential(
                    stored.username(),
                    stored.descriptor(),
                    updated
            );
        });
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(
            String username
    ) {
        return credentialsById.values().stream()
                .filter(value -> value.username().equals(username))
                .map(StoredCredential::descriptor)
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username))
                .map(UserIdentity::getId);
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return Optional.ofNullable(usernamesByHandle.get(userHandle));
    }

    @Override
    public Optional<RegisteredCredential> lookup(
            ByteArray credentialId,
            ByteArray userHandle
    ) {
        StoredCredential stored = credentialsById.get(credentialId);

        if (stored == null ||
                !stored.credential().getUserHandle().equals(userHandle)) {
            return Optional.empty();
        }

        return Optional.of(stored.credential());
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        StoredCredential stored = credentialsById.get(credentialId);

        if (stored == null) {
            return Set.of();
        }

        return Set.of(stored.credential());
    }

    private record StoredCredential(
            String username,
            PublicKeyCredentialDescriptor descriptor,
            RegisteredCredential credential
    ) {
    }
}
