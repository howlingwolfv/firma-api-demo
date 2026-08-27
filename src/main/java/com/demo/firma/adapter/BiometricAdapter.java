package com.demo.firma.adapter;

import com.demo.firma.dto.BiometricRequest;

public interface BiometricAdapter {
    BiometricResult validate(BiometricRequest request);
    record BiometricResult(boolean validated, String transactionId, String signerName, String mode) {}
}
