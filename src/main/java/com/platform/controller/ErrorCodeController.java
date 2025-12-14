package com.platform.controller;

import com.platform.dto.ErrorCodeCategoryRequest;
import com.platform.dto.ErrorCodeDTO;
import com.platform.dto.ErrorCodeRequest;
import com.platform.entity.ErrorCode;
import com.platform.entity.ErrorCodeAudit;
import com.platform.entity.ErrorCodeCategory;
import com.platform.entity.ErrorCodeVersion;
import com.platform.service.ErrorCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/error-codes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ErrorCodeController {

    private final ErrorCodeService errorCodeService;

    // ==================== ERROR CODE ENDPOINTS ====================

    @PostMapping
    public ResponseEntity<ErrorCodeDTO> createErrorCode(@RequestBody ErrorCodeRequest request) {
        return ResponseEntity.ok(errorCodeService.createErrorCode(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ErrorCodeDTO> updateErrorCode(@PathVariable Long id, @RequestBody ErrorCodeRequest request) {
        return ResponseEntity.ok(errorCodeService.updateErrorCode(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteErrorCode(@PathVariable Long id) {
        errorCodeService.deleteErrorCode(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ErrorCodeDTO> getErrorCode(@PathVariable Long id) {
        return ResponseEntity.ok(errorCodeService.getErrorCode(id));
    }

    @GetMapping
    public ResponseEntity<Page<ErrorCodeDTO>> getAllErrorCodes(
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) ErrorCode.ErrorSeverity severity,
            @RequestParam(required = false) ErrorCode.ErrorStatus status,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(errorCodeService.getAllErrorCodes(appName, categoryId, severity, status, search, pageable));
    }

    @GetMapping("/apps")
    public ResponseEntity<List<String>> getDistinctApps() {
        return ResponseEntity.ok(errorCodeService.getDistinctApps());
    }

    // ==================== CATEGORY ENDPOINTS ====================

    @PostMapping("/categories")
    public ResponseEntity<ErrorCodeCategory> createCategory(@RequestBody ErrorCodeCategoryRequest request) {
        return ResponseEntity.ok(errorCodeService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ErrorCodeCategory> updateCategory(@PathVariable Long id, @RequestBody ErrorCodeCategoryRequest request) {
        return ResponseEntity.ok(errorCodeService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        errorCodeService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    public ResponseEntity<List<ErrorCodeCategory>> getAllCategories(@RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(errorCodeService.getAllCategories(activeOnly));
    }

    // ==================== VERSION & AUDIT ENDPOINTS ====================

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<ErrorCodeVersion>> getVersionHistory(@PathVariable Long id) {
        return ResponseEntity.ok(errorCodeService.getVersionHistory(id));
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<ErrorCodeAudit>> getAuditLog(@PathVariable Long id) {
        return ResponseEntity.ok(errorCodeService.getAuditLog(id));
    }

    @PostMapping("/{id}/restore/{versionNumber}")
    public ResponseEntity<ErrorCodeDTO> restoreVersion(@PathVariable Long id, @PathVariable Integer versionNumber) {
        return ResponseEntity.ok(errorCodeService.restoreVersion(id, versionNumber));
    }
}
