package com.platform.controller;

import com.platform.dto.*;
import com.platform.enums.PermissionAction;
import com.platform.enums.PermissionResource;
import com.platform.media.model.AccessType;
import com.platform.security.RequirePermission;
import com.platform.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    @RequirePermission(resource = PermissionResource.MEDIA, action = PermissionAction.CREATE)
    public ResponseEntity<MediaFileDTO> uploadFile(
            @ModelAttribute MediaUploadRequest request,
            @RequestParam(required = false) Long appId) {
        MediaFileDTO result = mediaService.uploadFile(request, appId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/files")
    @RequirePermission(resource = PermissionResource.MEDIA, action = PermissionAction.READ)
    public ResponseEntity<Page<MediaFileDTO>> getFiles(
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String mimeType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long appId,
            Pageable pageable) {
        Page<MediaFileDTO> files = mediaService.getFiles(folderId, mimeType, search, appId, pageable);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/files/{id:[0-9]+}")
    @RequirePermission(resource = PermissionResource.MEDIA, action = PermissionAction.READ)
    public ResponseEntity<MediaFileDTO> getFile(
            @PathVariable Long id,
            @RequestParam(required = false) Long appId) {
        MediaFileDTO file = mediaService.getFile(id, appId);
        return ResponseEntity.ok(file);
    }

    @DeleteMapping("/files/{id:[0-9]+}")
    @RequirePermission(resource = PermissionResource.MEDIA, action = PermissionAction.DELETE)
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long id,
            @RequestParam(required = false) Long appId) {
        mediaService.deleteFile(id, appId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/files/{id:[0-9]+}/url")
    @RequirePermission(resource = PermissionResource.MEDIA, action = PermissionAction.READ)
    public ResponseEntity<Map<String, String>> generateFileUrl(
            @PathVariable Long id,
            @RequestParam(defaultValue = "PUBLIC") AccessType accessType,
            @RequestParam(required = false) Long appId) {
        String url = mediaService.generateFileUrl(id, accessType, appId);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/files/{id:[0-9]+}/download")
    @RequirePermission(resource = PermissionResource.MEDIA, action = PermissionAction.READ)
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable Long id,
            @RequestParam(required = false) Long appId) {
        return mediaService.downloadFile(id, appId);
    }

    @GetMapping("/files/{filename:[a-f0-9\\-]+\\.[a-zA-Z0-9]+}")
    public ResponseEntity<byte[]> serveStaticFile(@PathVariable String filename) {
        return mediaService.serveStaticFile(filename);
    }

    // Folder Management Endpoints
    @GetMapping("/folders")
    @RequirePermission(resource = PermissionResource.MEDIA, action = PermissionAction.READ)
    public ResponseEntity<List<MediaFolderDTO>> getFolders(
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Long appId) {
        List<MediaFolderDTO> folders = mediaService.getFolders(parentId, appId);
        return ResponseEntity.ok(folders);
    }

    @PostMapping("/folders")
    @RequirePermission(resource = PermissionResource.MEDIA, action = PermissionAction.CREATE)
    public ResponseEntity<MediaFolderDTO> createFolder(
            @RequestBody CreateFolderRequest request,
            @RequestParam(required = false) Long appId) {
        MediaFolderDTO folder = mediaService.createFolder(request, appId);
        return ResponseEntity.ok(folder);
    }

    @PutMapping("/folders/{id:[0-9]+}")
    @RequirePermission(resource = PermissionResource.MEDIA, action = PermissionAction.UPDATE)
    public ResponseEntity<MediaFolderDTO> updateFolder(
            @PathVariable Long id,
            @RequestBody UpdateFolderRequest request,
            @RequestParam(required = false) Long appId) {
        MediaFolderDTO folder = mediaService.updateFolder(id, request, appId);
        return ResponseEntity.ok(folder);
    }

    @DeleteMapping("/folders/{id:[0-9]+}")
    @RequirePermission(resource = PermissionResource.MEDIA, action = PermissionAction.DELETE)
    public ResponseEntity<Void> deleteFolder(
            @PathVariable Long id,
            @RequestParam(required = false) Long appId) {
        mediaService.deleteFolder(id, appId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/folders/{id:[0-9]+}/tree")
    @RequirePermission(resource = PermissionResource.MEDIA, action = PermissionAction.READ)
    public ResponseEntity<MediaFolderDTO> getFolderTree(
            @PathVariable Long id,
            @RequestParam(required = false) Long appId) {
        MediaFolderDTO folderTree = mediaService.getFolderTree(id, appId);
        return ResponseEntity.ok(folderTree);
    }

    // Storage Provider Management Endpoints
    @GetMapping("/providers/current")
    @RequirePermission(resource = PermissionResource.MEDIA, action = PermissionAction.READ)
    public ResponseEntity<Map<String, Object>> getCurrentStorageProvider(
            @RequestParam(required = false) Long appId) {
        Map<String, Object> currentProvider = mediaService.getCurrentStorageProvider(appId);
        return ResponseEntity.ok(currentProvider);
    }

    @GetMapping("/providers")
    @RequirePermission(resource = PermissionResource.MEDIA, action = PermissionAction.READ)
    public ResponseEntity<List<Map<String, Object>>> getStorageProviders(
            @RequestParam(required = false) Long appId) {
        List<Map<String, Object>> providers = mediaService.getStorageProviders(appId);
        return ResponseEntity.ok(providers);
    }

    @PostMapping("/providers")
    @RequirePermission(resource = PermissionResource.MEDIA, action = PermissionAction.CREATE)
    public ResponseEntity<Map<String, Object>> saveStorageProvider(
            @RequestBody Map<String, Object> providerConfig,
            @RequestParam(required = false) Long appId) {
        Map<String, Object> savedProvider = mediaService.saveStorageProvider(providerConfig, appId);
        return ResponseEntity.ok(savedProvider);
    }

    @PostMapping("/providers/test")
    @RequirePermission(resource = PermissionResource.MEDIA, action = PermissionAction.READ)
    public ResponseEntity<Map<String, Object>> testStorageProvider(
            @RequestBody Map<String, Object> providerConfig,
            @RequestParam(required = false) Long appId) {
        Map<String, Object> testResult = mediaService.testStorageProvider(providerConfig, appId);
        return ResponseEntity.ok(testResult);
    }
}