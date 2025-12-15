package com.platform.media.provider;

import com.platform.media.model.UploadResult;
import com.platform.media.model.FilePayload;
import com.platform.media.model.MediaUrl;
import com.platform.media.model.AccessType;
import com.platform.media.model.FileMetadata;

public interface MediaProvider {
    
    /**
     * Upload a file to the provider
     */
    UploadResult upload(FilePayload payload);
    
    /**
     * Delete a file from the provider
     */
    void delete(String providerKey);
    
    /**
     * Generate URL for accessing the file
     */
    MediaUrl generateUrl(String providerKey, AccessType accessType);
    
    /**
     * Check if file exists
     */
    boolean exists(String providerKey);
    
    /**
     * Get file metadata
     */
    FileMetadata getMetadata(String providerKey);
    
    /**
     * Get provider type
     */
    String getProviderType();
    
    /**
     * Validate provider configuration
     */
    boolean validateConfiguration();
}