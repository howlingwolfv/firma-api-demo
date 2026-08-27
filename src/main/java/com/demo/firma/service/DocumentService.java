package com.demo.firma.service;

import com.demo.firma.exception.DomainException;
import com.demo.firma.model.SignatureOperation;
import com.demo.firma.model.SignatureStatus;
import com.demo.firma.storage.BlobStorageService;
import com.demo.firma.util.HashUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@ApplicationScoped
public class DocumentService {

    @Inject
    BlobStorageService blobStorageService;

    public void attachOriginalPdf(SignatureOperation operation, byte[] pdf) {
        if (operation.getStatus() != SignatureStatus.CREATED) {
            throw new DomainException(409, "El documento solo puede adjuntarse a una operación CREATED");
        }

        validatePdf(pdf);

        String blobName = "original/" + operation.getSignatureId() + ".pdf";
        blobStorageService.upload(blobName, pdf, false);

        operation.setOriginalBlobName(blobName);
        operation.setAcceptedDocumentHash(HashUtils.sha256(pdf));
        operation.setStatus(SignatureStatus.DOCUMENT_READY);
        operation.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    public byte[] getOriginalPdf(SignatureOperation operation) {
        if (operation.getOriginalBlobName() == null) {
            throw new DomainException(404, "La operación todavía no tiene documento");
        }
        return blobStorageService.download(operation.getOriginalBlobName());
    }

    public byte[] getSignedPdf(SignatureOperation operation) {
        if (operation.getStatus() != SignatureStatus.SIGNED || operation.getSignedBlobName() == null) {
            throw new DomainException(409, "La operación todavía no tiene un PDF final");
        }
        return blobStorageService.download(operation.getSignedBlobName());
    }

    private void validatePdf(byte[] pdf) {
        if (pdf == null || pdf.length < 5) {
            throw new DomainException(400, "Debe enviarse un archivo PDF válido");
        }
        String header = new String(pdf, 0, 5, StandardCharsets.US_ASCII);
        if (!"%PDF-".equals(header)) {
            throw new DomainException(400, "El contenido enviado no parece ser un PDF");
        }
    }
}
