package com.demo.firma.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record WebAuthnRegistrationVerifyRequest(
        String username,
        JsonNode credential
) {
}
