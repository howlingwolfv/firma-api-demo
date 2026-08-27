package com.demo.firma.adapter;

import com.demo.firma.dto.BiometricRequest;
import com.demo.firma.exception.DomainException;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class DemoBiometricAdapter implements BiometricAdapter {
    @Override
    public BiometricResult validate(BiometricRequest request) {
        if (request == null || !request.approved()) {
            throw new DomainException(400, "La biometría demo debe ser aprobada para continuar");
        }
        String signerName = request.signerName() == null || request.signerName().isBlank()
                ? "Usuario Demo"
                : request.signerName().trim();
        return new BiometricResult(true, "BIO-DEMO-" + shortId(), signerName, "DEMO");
    }
    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
