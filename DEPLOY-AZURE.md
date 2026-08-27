# Valores para el despliegue del POC

Container App: `ca-firma-api-demo`

URL actual:
`https://ca-firma-api-demo.agreeablehill-d2ec393a.westcentralus.azurecontainerapps.io`

Al reemplazar la imagen quickstart:
- Target port: `8080`
- Min replicas: `0`
- Max replicas: `1`
- Managed Identity: `Enabled`

Variables de entorno:
```text
AZURE_STORAGE_ACCOUNT_NAME=stfirmademo
AZURE_STORAGE_CONTAINER=documents
```

Rol ya configurado en Storage:
`Storage Blob Data Contributor`
