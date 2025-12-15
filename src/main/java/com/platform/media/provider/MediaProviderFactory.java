package com.platform.media.provider;

import com.platform.entity.MediaFile;
import com.platform.entity.MediaProviderConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaProviderFactory {

    private final LocalFileSystemProvider localProvider;
    // Future providers will be injected here

    public MediaProvider getProvider(MediaFile.MediaProviderType providerType) {
        return getProvider(providerType, null);
    }

    public MediaProvider getProvider(MediaFile.MediaProviderType providerType, MediaProviderConfig config) {
        switch (providerType) {
            case LOCAL:
                return localProvider;
            case CLOUDFLARE_R2:
                // TODO: Implement CloudflareR2Provider
                throw new UnsupportedOperationException("Cloudflare R2 provider not yet implemented");
            case AWS_S3:
                // TODO: Implement S3Provider
                throw new UnsupportedOperationException("AWS S3 provider not yet implemented");
            case GOOGLE_DRIVE:
                // TODO: Implement GoogleDriveProvider
                throw new UnsupportedOperationException("Google Drive provider not yet implemented");
            case DROPBOX:
                // TODO: Implement DropboxProvider
                throw new UnsupportedOperationException("Dropbox provider not yet implemented");
            case AZURE_BLOB:
                // TODO: Implement AzureBlobProvider
                throw new UnsupportedOperationException("Azure Blob provider not yet implemented");
            default:
                log.warn("Unknown provider type: {}, falling back to LOCAL", providerType);
                return localProvider;
        }
    }

    public MediaProvider getDefaultProvider() {
        return localProvider;
    }

    public boolean isProviderSupported(MediaFile.MediaProviderType providerType) {
        switch (providerType) {
            case LOCAL:
                return true;
            case CLOUDFLARE_R2:
            case AWS_S3:
            case GOOGLE_DRIVE:
            case DROPBOX:
            case AZURE_BLOB:
                return false; // Not yet implemented
            default:
                return false;
        }
    }
}