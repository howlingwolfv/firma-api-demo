# firma-api-demo - Backend POC

Backend Java 21 + Quarkus para la demostración del Motor de Firma Electrónica.

## Implementado

1. Creación de operación de firma.
2. Carga de PDF original.
3. Cálculo SHA-256 del PDF.
4. Persistencia del PDF original en Azure Blob Storage.
5. Registro de consentimiento.
6. Biometría simulada mediante adapter.
7. Registro real de passkey mediante WebAuthn.
8. Generación de opciones WebAuthn para firma.
9. Validación criptográfica WebAuthn/FIDO2 en backend usando `com.yubico:webauthn-server-core:2.9.0`.
10. Vinculación de la assertion FIDO2 con el contexto de la operación.
11. Generación del PDF final con constancia mediante Apache PDFBox.
12. Persistencia del PDF final en Blob Storage.
13. Expediente JSON de evidencias en Blob Storage.
14. Consulta de estado.
15. Descarga del PDF original y final.

## Estado actual del POC

La biometría continúa simulada mediante `DemoBiometricAdapter`.

FIDO2/WebAuthn ya no utiliza el esquema anterior:

```json
{
  "approved": true,
  "credentialId": "demo-passkey-001"
}
```

La confirmación FIDO2 ahora se realiza mediante una ceremonia WebAuthn real en navegador y se valida criptográficamente en backend.

La implementación usa:

```text
com.yubico:webauthn-server-core:2.9.0
```

El flujo WebAuthn valida challenge, origin, RP ID, credentialId, firma criptográfica y user verification.

## Arquitectura de adapters

La biometría permanece desacoplada para poder sustituir posteriormente:

```text
DemoBiometricAdapter
        ↓
BIOM corporativo
```

La implementación FIDO2/WebAuthn queda preparada para evolucionar posteriormente hacia Mosaic / WebAuthn corporativo sin cambiar el flujo principal del Motor de Firma.

## Persistencia del POC

Las operaciones se almacenan en memoria.

Por tanto, un reinicio del backend o una nueva revisión de Azure Container Apps elimina:

- operaciones activas;
- usuarios WebAuthn registrados;
- claves públicas registradas;
- signature counters;
- challenges / ceremonies pendientes.

Los PDFs y evidencias almacenados en Azure Blob Storage permanecen.

Para producción, el estado transaccional y las credenciales WebAuthn deben persistirse, por ejemplo, en Azure SQL u otro repositorio persistente.

Las ceremonies/challenges pendientes deberían almacenarse en un repositorio temporal apropiado.

## Variables de entorno

### Azure Storage

```text
AZURE_STORAGE_ACCOUNT_NAME=stfirmademo
AZURE_STORAGE_CONTAINER=documents
```

### CORS

```text
CORS_ORIGINS=https://web-firma-demo-dheweb0cpg6djek.westcentralus-01.azurewebsites.net
```

El origin debe coincidir exactamente con el dominio desde el que se ejecuta Angular.

No debe terminar en `/`.

### WebAuthn

```text
WEBAUTHN_RP_ID=web-firma-demo-dheweb0cpg6djek.westcentralus-01.azurewebsites.net
WEBAUTHN_RP_NAME=Motor de Firma Electronica Demo
WEBAUTHN_RP_ORIGIN=https://web-firma-demo-dheweb0cpg6djek.westcentralus-01.azurewebsites.net
WEBAUTHN_CEREMONY_TTL_SECONDS=300
```

Consideraciones:

- `WEBAUTHN_RP_ID` no lleva `https://`.
- `WEBAUTHN_RP_ORIGIN` sí lleva `https://`.
- `WEBAUTHN_RP_ORIGIN` no debe terminar en `/`.
- La ceremonia WebAuthn necesita un origen HTTPS válido.
- Para desarrollo local, WebAuthn permite `localhost`.

## Ejecutar localmente

```bash
mvn quarkus:dev
```

Para compilar:

```bash
mvn clean package
```

## Flujo integral de firma

### 1. Crear operación

```text
POST /api/signatures
```

Estado inicial:

```text
CREATED
```

---

### 2. Adjuntar PDF original

```text
POST /api/signatures/{id}/document
```

Header:

```text
Content-Type: application/pdf
```

Body:

```text
archivo PDF binario
```

El backend:

- valida el PDF;
- calcula su SHA-256;
- lo almacena en Blob Storage;
- registra el hash dentro de la operación.

Estado:

```text
DOCUMENT_READY
```

---

### 3. Consultar PDF original

```text
GET /api/signatures/{id}/document
```

Devuelve:

```text
Content-Type: application/pdf
```

---

### 4. Registrar consentimiento

```text
POST /api/signatures/{id}/consent
```

Ejemplo:

```json
{
  "accepted": true,
  "consentText": "He leído el documento y acepto firmarlo electrónicamente."
}
```

El backend registra el hash del texto de consentimiento y la fecha/hora.

Estado:

```text
CONSENTED
```

---

### 5. Validar biometría DEMO

```text
POST /api/signatures/{id}/biometric
```

Ejemplo:

```json
{
  "approved": true,
  "signerName": "Usuario Demo"
}
```

En esta fase la biometría continúa simulada.

Estado:

```text
BIOMETRIC_VALIDATED
```

---

# WebAuthn / FIDO2 real

La implementación WebAuthn consta de dos procesos:

1. registro de una passkey;
2. confirmación de una operación de firma.

## 6. Registro de passkey

Este proceso se realiza una vez por usuario mientras la credencial permanezca registrada en el backend.

### 6.1 Obtener opciones de registro

```text
POST /api/webauthn/register/options
```

El backend genera `PublicKeyCredentialCreationOptions`.

El frontend convierte los campos Base64URL correspondientes a `ArrayBuffer` y ejecuta:

```javascript
navigator.credentials.create(...)
```

El navegador puede solicitar:

- Windows Hello;
- huella;
- reconocimiento facial;
- PIN;
- passkey disponible en el dispositivo.

### 6.2 Verificar registro

```text
POST /api/webauthn/register/verify
```

El frontend serializa la respuesta de `navigator.credentials.create()` a Base64URL y la envía al backend.

El backend valida:

- challenge;
- origin;
- RP ID;
- respuesta criptográfica;
- credentialId;
- public key;
- signature counter.

Si es correcto, registra la credencial WebAuthn.

---

## 7. Generar opciones WebAuthn para la firma

Después de alcanzar:

```text
BIOMETRIC_VALIDATED
```

se llama:

```text
POST /api/signatures/{id}/webauthn/options
```

El backend genera una `AssertionRequest` vinculada a la operación.

La vinculación utiliza un hash del contexto:

```text
signatureId
+
acceptedDocumentHash
+
consentTextHash
+
biometricTransactionId
```

La `AssertionRequest` pendiente queda asociada al `signatureId`.

Si ese contexto cambia antes de verificar la assertion, la operación es rechazada.

El frontend utiliza las opciones devueltas para ejecutar:

```javascript
navigator.credentials.get(...)
```

---

## 8. Verificar WebAuthn/FIDO2

```text
POST /api/signatures/{id}/webauthn/verify
```

El frontend serializa la respuesta de:

```javascript
navigator.credentials.get(...)
```

y la envía al backend.

La librería Yubico valida:

- challenge;
- origin;
- RP ID;
- credentialId;
- firma criptográfica;
- user presence;
- user verification;
- signature counter.

Solo si la assertion es válida, la operación pasa a:

```text
FIDO2_CONFIRMED
```

---

## 9. Finalizar firma

```text
POST /api/signatures/{id}/finalize
```

Sin body.

El backend:

1. obtiene el PDF original;
2. genera la constancia de firma electrónica mediante PDFBox;
3. genera el PDF final;
4. almacena el PDF final en Blob Storage;
5. genera el expediente JSON de evidencias;
6. almacena las evidencias;
7. cambia el estado a `SIGNED`.

Estado final:

```text
SIGNED
```

---

## 10. Consultar estado

```text
GET /api/signatures/{id}
```

Estados posibles:

```text
CREATED
  -> DOCUMENT_READY
  -> CONSENTED
  -> BIOMETRIC_VALIDATED
  -> FIDO2_CONFIRMED
  -> SIGNED
```

---

## 11. Descargar PDF final

```text
GET /api/signatures/{id}/signed-document
```

Devuelve el PDF final generado por el Motor de Firma.

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

## Endpoints técnicos

```text
GET  /api
GET  /api/health
POST /api/storage/test

GET /q/health
GET /q/health/live
GET /q/health/ready
```

Los endpoints `/q/...` son endpoints técnicos expuestos por Quarkus.

`/api/storage/test` se utiliza solamente para validar acceso a Azure Blob Storage durante el POC.

## Pruebas

### Postman

Postman continúa siendo útil para probar:

- health;
- creación de operación;
- carga de PDF;
- consentimiento;
- biometría demo;
- generación de opciones WebAuthn;
- consulta de estados;
- finalización;
- descarga del PDF.

Sin embargo, una ceremonia WebAuthn completa no puede ejecutarse únicamente desde Postman.

### Navegador

Estos pasos necesitan ejecutarse desde Angular o JavaScript en navegador:

```javascript
navigator.credentials.create()
```

y:

```javascript
navigator.credentials.get()
```

El navegador debe ejecutarse desde:

- un origen HTTPS válido; o
- `localhost` durante desarrollo.

## Flujo completo actual

```text
Crear operación
      ↓
Cargar PDF
      ↓
SHA-256 + Blob
      ↓
Consentimiento
      ↓
Biometría DEMO
      ↓
Registro Passkey WebAuthn
      ↓
WebAuthn options de firma
      ↓
navigator.credentials.get()
      ↓
Validación criptográfica en Quarkus
      ↓
FIDO2_CONFIRMED
      ↓
PDFBox
      ↓
PDF final + evidencias
      ↓
SIGNED
```

## Próximos pasos

Para acercar el POC al modelo productivo:

1. actualizar Angular para manejar Base64URL / `ArrayBuffer`;
2. ejecutar `navigator.credentials.create()` para registrar passkeys;
3. ejecutar `navigator.credentials.get()` para confirmar firmas;
4. sustituir `DemoBiometricAdapter` por BIOM corporativo;
5. persistir operaciones y credenciales WebAuthn;
6. sustituir o integrar la implementación WebAuthn con Mosaic/FIDO2 corporativo;
7. incorporar OTP cuando la política de firma lo requiera.
