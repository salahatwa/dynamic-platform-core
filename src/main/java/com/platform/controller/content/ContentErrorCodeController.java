package com.platform.controller.content;

import com.platform.dto.ErrorCodeContentResponse;
import com.platform.entity.ErrorCode;
import com.platform.entity.ErrorCodeTranslation;
import com.platform.repository.ErrorCodeRepository;
import com.platform.repository.ErrorCodeTranslationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/content/error-codes")
@RequiredArgsConstructor
@Tag(name = "Content API - Error Codes", description = "Public API for accessing error codes with API key authentication")
@SecurityRequirement(name = "ApiKey")
public class ContentErrorCodeController {

    private final ErrorCodeRepository errorCodeRepository;
    private final ErrorCodeTranslationRepository translationRepository;

    @GetMapping("/page")
    @Operation(
        summary = "Get error codes for app",
        description = "Retrieve error codes with all translations for the authenticated app. Supports filtering, sorting, and pagination."
    )
    public ResponseEntity<Page<ErrorCodeContentResponse>> getErrorCodes(
            @Parameter(description = "Filter by category ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter by severity (INFO, WARNING, ERROR, CRITICAL)") @RequestParam(required = false) String severity,
            @Parameter(description = "Filter by status (ACTIVE, DEPRECATED, REMOVED)") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by module name") @RequestParam(required = false) String module,
            @Parameter(description = "Search in error code or message") @RequestParam(required = false) String search,
            @Parameter(description = "Sort field (errorCode, severity, status, createdAt)") @RequestParam(defaultValue = "errorCode") String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        // Create sort - map field names to database column names for native queries
        String dbSortField = mapSortFieldToColumn(sortBy);
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, dbSortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Get current app from security context (set by API key filter)
        Long appId = getCurrentAppId();
        
        // Build query with filters
        Page<ErrorCode> errorCodes = errorCodeRepository.findAllWithFiltersForContent(
            appId, categoryId, severity, status, module, search, pageable);

        // Convert to response DTOs with translations
        Page<ErrorCodeContentResponse> response = errorCodes.map(this::mapToContentResponse);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
        summary = "Get all error codes for app",
        description = "Retrieve all error codes with all translations for the authenticated app. Supports filtering and sorting but returns complete list without pagination."
    )
    public ResponseEntity<List<ErrorCodeContentResponse>> getAllErrorCodes(
            @Parameter(description = "Filter by category ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter by severity (INFO, WARNING, ERROR, CRITICAL)") @RequestParam(required = false) String severity,
            @Parameter(description = "Filter by status (ACTIVE, DEPRECATED, REMOVED)") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by module name") @RequestParam(required = false) String module,
            @Parameter(description = "Search in error code or message") @RequestParam(required = false) String search,
            @Parameter(description = "Sort field (errorCode, severity, status, createdAt)") @RequestParam(defaultValue = "errorCode") String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(defaultValue = "asc") String sortDir) {

        // Create sort - map field names to database column names for native queries
        String dbSortField = mapSortFieldToColumn(sortBy);
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, dbSortField);
        
        Long appId = getCurrentAppId();
        
        // Use a large page size to get all results
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, sort);
        Page<ErrorCode> errorCodes = errorCodeRepository.findAllWithFiltersForContent(
            appId, categoryId, severity, status, module, search, pageable);

        // Convert to response DTOs with translations
        List<ErrorCodeContentResponse> response = errorCodes.getContent().stream()
            .map(this::mapToContentResponse)
            .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{errorCode}")
    @Operation(
        summary = "Get specific error code",
        description = "Retrieve a specific error code with all translations by error code value"
    )
    public ResponseEntity<ErrorCodeContentResponse> getErrorCode(
            @Parameter(description = "Error code value") @PathVariable String errorCode) {

        Long appId = getCurrentAppId();
        
        ErrorCode error = errorCodeRepository.findByErrorCodeAndAppId(errorCode, appId)
            .orElseThrow(() -> new RuntimeException("Error code not found"));

        return ResponseEntity.ok(mapToContentResponse(error));
    }

    @GetMapping("/categories")
    @Operation(
        summary = "Get error code categories",
        description = "Retrieve all error code categories for the authenticated app"
    )
    public ResponseEntity<List<Map<String, Object>>> getCategories() {
        Long appId = getCurrentAppId();
        
        List<Map<String, Object>> categories = errorCodeRepository.findCategoriesByAppId(appId);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/modules")
    @Operation(
        summary = "Get error code modules",
        description = "Retrieve all distinct module names for the authenticated app"
    )
    public ResponseEntity<List<String>> getModules() {
        Long appId = getCurrentAppId();
        
        List<String> modules = errorCodeRepository.findDistinctModulesByAppId(appId);
        return ResponseEntity.ok(modules);
    }

    private ErrorCodeContentResponse mapToContentResponse(ErrorCode errorCode) {
        // Get all translations for this error code
        List<ErrorCodeTranslation> translations = translationRepository.findByErrorCodeId(errorCode.getId());
        
        Map<String, ErrorCodeContentResponse.TranslationData> translationMap = translations.stream()
            .collect(Collectors.toMap(
                ErrorCodeTranslation::getLanguageCode,
                t -> ErrorCodeContentResponse.TranslationData.builder()
                    .message(t.getMessage())
                    .technicalDetails(t.getTechnicalDetails())
                    .resolutionSteps(t.getResolutionSteps())
                    .build()
            ));

        return ErrorCodeContentResponse.builder()
            .id(errorCode.getId())
            .errorCode(errorCode.getErrorCode())
            .severity(errorCode.getSeverity())
            .status(errorCode.getStatus())
            .httpStatusCode(errorCode.getHttpStatusCode())
            .isPublic(errorCode.getIsPublic())
            .isRetryable(errorCode.getIsRetryable())
            .defaultMessage(errorCode.getDefaultMessage())
            .technicalDetails(errorCode.getTechnicalDetails())
            .resolutionSteps(errorCode.getResolutionSteps())
            .documentationUrl(errorCode.getDocumentationUrl())
            .moduleName(errorCode.getModuleName())
            .categoryId(errorCode.getCategory() != null ? errorCode.getCategory().getId() : null)
            .categoryName(errorCode.getCategory() != null ? errorCode.getCategory().getCategoryName() : null)
            .translations(translationMap)
            .createdAt(errorCode.getCreatedAt())
            .updatedAt(errorCode.getUpdatedAt())
            .build();
    }

    private String mapSortFieldToColumn(String sortBy) {
        // Map API field names to database column names for native queries
        return switch (sortBy) {
            case "errorCode" -> "error_code";
            case "createdAt" -> "created_at";
            case "updatedAt" -> "updated_at";
            case "moduleName" -> "module_name";
            case "httpStatusCode" -> "http_status_code";
            case "isPublic" -> "is_public";
            case "isRetryable" -> "is_retryable";
            case "defaultMessage" -> "default_message";
            case "technicalDetails" -> "technical_details";
            case "resolutionSteps" -> "resolution_steps";
            case "documentationUrl" -> "documentation_url";
            case "corporateId" -> "corporate_id";
            default -> sortBy; // For fields that match (severity, status, etc.)
        };
    }

    private Long getCurrentAppId() {
        // This will be set by the API key authentication filter
        // For now, we'll get it from the request attribute
        return (Long) org.springframework.web.context.request.RequestContextHolder
            .currentRequestAttributes()
            .getAttribute("appId", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
    }
}