# firma-api-demo - Backend POC

Backend Java 21 + Quarkus para la demostracion del Motor de Firma Electronica.

## Implementado

1. Creacion de operacion de firma.
2. Carga de PDF original.
3. SHA-256 del PDF.
4. Persistencia del PDF original en Azure Blob Storage.
5. Consentimiento.
6. Biometria simulada mediante adapter.
7. Challenge FIDO2 asociado al contexto de la operacion.
8. Verificacion FIDO2 simulada mediante adapter.
9. Generacion del PDF final con constancia mediante Apache PDFBox.
10. Persistencia del PDF final en Blob Storage.
11. Expediente JSON de evidencias en Blob Storage.
12. Consulta de estado.
13. Descarga del PDF original y final.

## Importante

BIOM y FIDO2 son simulados en esta fase.

Los adapters estan separados para poder sustituir posteriormente:

- `DemoBiometricAdapter` por BIOM corporativo.
- `DemoFido2Adapter` por Mosaic / WebAuthn corporativo.

`approved=true` solo existe para el POC y no representa una validacion de seguridad productiva.

Las operaciones se almacenan en memoria. Un reinicio o nueva revision de Container Apps elimina el estado transaccional del POC. Los PDFs y evidencias almacenados en Blob permanecen.

En produccion el estado debe persistirse en Azure SQL u otro repositorio transaccional.

## Variables

```text
AZURE_STORAGE_ACCOUNT_NAME=stfirmademo
AZURE_STORAGE_CONTAINER=documents
CORS_ORIGINS=*
FIDO2_CHALLENGE_TTL_SECONDS=300
```

## Ejecutar

```bash
mvn quarkus:dev
```

## Flujo de prueba

### 1. Crear operacion

`POST /api/signatures`

### 2. Adjuntar PDF

`POST /api/signatures/{id}/document`

Header:

```text
Content-Type: application/pdf
```

Body: archivo PDF binario.

### 3. Consultar PDF original

`GET /api/signatures/{id}/document`

### 4. Consentimiento

`POST /api/signatures/{id}/consent`

```json
{
  "accepted": true,
  "consentText": "He leido el documento y acepto firmarlo electronicamente."
}
```

### 5. Biometria demo

`POST /api/signatures/{id}/biometric`

```json
{
  "approved": true,
  "signerName": "Usuario Demo"
}
```

### 6. Challenge

`POST /api/signatures/{id}/webauthn/challenge`

Sin body.

### 7. FIDO2 demo

`POST /api/signatures/{id}/webauthn/verify`

```json
{
  "approved": true,
  "credentialId": "demo-passkey-001"
}
```

### 8. Finalizar

`POST /api/signatures/{id}/finalize`

Sin body.

### 9. Estado

`GET /api/signatures/{id}`

El estado final debe ser `SIGNED`.

### 10. PDF final

`GET /api/signatures/{id}/signed-document`

## Estados

```text
CREATED
  -> DOCUMENT_READY
  -> CONSENTED
  -> BIOMETRIC_VALIDATED
  -> FIDO2_CONFIRMED
  -> SIGNED
```

## Blob Storage esperado

```text
documents/
  original/
    SIG-XXXXXXXX.pdf
  signed/
    SIG-XXXXXXXX.pdf
  evidence/
    SIG-XXXXXXXX.json
  test/
    test-....txt
```
