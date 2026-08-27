package com.demo.firma.repository;

import com.demo.firma.model.SignatureOperation;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class SignatureRepository {
    private final ConcurrentMap<String, SignatureOperation> operations = new ConcurrentHashMap<>();
    public void save(SignatureOperation operation) { operations.put(operation.getSignatureId(), operation); }
    public Optional<SignatureOperation> findById(String signatureId) { return Optional.ofNullable(operations.get(signatureId)); }
}
