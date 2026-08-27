package com.demo.firma.service;

import com.demo.firma.model.SignatureOperation;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class PdfService {

    private static final PDType1Font FONT =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    private static final PDType1Font FONT_BOLD =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] appendElectronicSignatureCertificate(byte[] originalPdf, SignatureOperation operation) {
        try (PDDocument document = Loader.loadPDF(originalPdf);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 790;

                writeLine(content, FONT_BOLD, 17, 50, y, "CONSTANCIA DE FIRMA ELECTRONICA");
                y -= 45;
                writeLine(content, FONT, 11, 50, y, "Operacion: " + operation.getSignatureId());
                y -= 22;
                writeLine(content, FONT, 11, 50, y, "Firmante: " + safe(operation.getSignerName()));
                y -= 22;
                writeLine(content, FONT, 11, 50, y, "Biometria facial: VALIDADA - DEMO");
                y -= 22;
                writeLine(content, FONT, 11, 50, y, "Prueba de vida: VALIDADA - DEMO");
                y -= 22;
                writeLine(content, FONT, 11, 50, y, "FIDO2 / Passkey: CONFIRMADO - DEMO");
                y -= 22;
                writeLine(content, FONT, 11, 50, y,
                        "Fecha de confirmacion: " + format(operation.getFido2ConfirmedAt()));

                y -= 35;
                writeLine(content, FONT_BOLD, 11, 50, y, "SHA-256 del documento aceptado:");
                y -= 20;

                String hash = operation.getAcceptedDocumentHash();
                if (hash != null && hash.length() > 40) {
                    writeLine(content, FONT, 9, 50, y, hash.substring(0, 40));
                    y -= 17;
                    writeLine(content, FONT, 9, 50, y, hash.substring(40));
                } else {
                    writeLine(content, FONT, 9, 50, y, safe(hash));
                }

                y -= 55;
                writeLine(content, FONT_BOLD, 13, 50, y, "FIRMADO ELECTRONICAMENTE");
                y -= 23;
                writeLine(content, FONT, 10, 50, y,
                        "Identidad + consentimiento + FIDO2 vinculados a esta operacion.");
                y -= 35;
                writeLine(content, FONT, 8, 50, y,
                        "POC tecnico. BIOM y FIDO2 se encuentran simulados mediante adapters demo.");
            }

            document.save(output);
            return output.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("No fue posible generar el PDF final", e);
        }
    }

    private void writeLine(PDPageContentStream content, PDType1Font font,
                           float fontSize, float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }

    private String format(OffsetDateTime value) {
        return value == null ? "-" : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value);
    }
}
