package com.demo.firma.storage;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@ApplicationScoped
public class BlobStorageService {

    @ConfigProperty(name = "azure.storage.account-name")
    String accountName;

    @ConfigProperty(name = "azure.storage.container")
    String containerName;

    private volatile BlobContainerClient containerClient;

    private BlobContainerClient container() {
        BlobContainerClient local = containerClient;
        if (local == null) {
            synchronized (this) {
                local = containerClient;
                if (local == null) {
                    String endpoint = "https://" + accountName + ".blob.core.windows.net";
                    local = new BlobContainerClientBuilder()
                            .endpoint(endpoint)
                            .containerName(containerName)
                            .credential(new DefaultAzureCredentialBuilder().build())
                            .buildClient();
                    containerClient = local;
                }
            }
        }
        return local;
    }

    public void upload(String blobName, byte[] content, boolean overwrite) {
        BlobClient blob = container().getBlobClient(blobName);
        blob.upload(new ByteArrayInputStream(content), content.length, overwrite);
    }

    public byte[] download(String blobName) {
        try {
            return container().getBlobClient(blobName).downloadContent().toBytes();
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == 404) {
                throw new IllegalArgumentException("Blob no encontrado: " + blobName, e);
            }
            throw e;
        }
    }

    public boolean exists(String blobName) {
        return container().getBlobClient(blobName).exists();
    }

    public String createTestBlob() {
        String blobName = "test/test-" + UUID.randomUUID() + ".txt";
        String text = "firma-api-demo OK " + OffsetDateTime.now(ZoneOffset.UTC);
        upload(blobName, text.getBytes(StandardCharsets.UTF_8), true);
        return blobName;
    }

    public String getAccountName() { return accountName; }
    public String getContainerName() { return containerName; }
}
