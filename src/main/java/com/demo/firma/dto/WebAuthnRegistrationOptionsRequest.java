package com.demo.firma.dto;

public record WebAuthnRegistrationOptionsRequest(
        String username,
        String displayName
) {
}
