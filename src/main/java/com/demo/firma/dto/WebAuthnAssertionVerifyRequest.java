package com.demo.firma.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record WebAuthnAssertionVerifyRequest(
        JsonNode credential
) {
}
