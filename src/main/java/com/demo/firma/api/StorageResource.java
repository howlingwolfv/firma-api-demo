package com.demo.firma.api;

import com.demo.firma.storage.BlobStorageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.LinkedHashMap;
import java.util.Map;

@Path("/api/storage")
@Produces(MediaType.APPLICATION_JSON)
public class StorageResource {

    @Inject
    BlobStorageService blobStorageService;

    @POST
    @Path("/test")
    public Map<String, Object> testStorage() {
        String blobName = blobStorageService.createTestBlob();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "OK");
        response.put("account", blobStorageService.getAccountName());
        response.put("container", blobStorageService.getContainerName());
        response.put("blobName", blobName);
        return response;
    }
}
