package com.platform.controller.content;

import com.platform.dto.AppConfigContentResponse;
import com.platform.entity.AppConfig;
import com.platform.repository.AppConfigRepository;
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

@RestController
@RequestMapping("/api/content/app-config")
@RequiredArgsConstructor
@Tag(name = "Content API - App Configuration", description = "Public API for accessing app configurations with API key authentication")
@SecurityRequirement(name = "ApiKey")
public class ContentAppConfigController {

    private final AppConfigRepository appConfigRepository;

    @GetMapping("/page")
    @Operation(
        summary = "Get app configurations",
        description = "Retrieve app configurations for the authenticated app. Supports filtering, sorting, and pagination."
    )
    public ResponseEntity<Page<AppConfigContentResponse>> getAppConfigs(
            @Parameter(description = "Filter by group name") @RequestParam(required = false) String group,
            @Parameter(description = "Filter by data type") @RequestParam(required = false) String dataType,
            @Parameter(description = "Filter by required status") @RequestParam(required = false) Boolean required,
            @Parameter(description = "Search in config key or description") @RequestParam(required = false) String search,
            @Parameter(description = "Sort field (configKey, group, dataType, createdAt)") @RequestParam(defaultValue = "configKey") String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        // Create sort - map field names to database column names for native queries
        String dbSortField = mapSortFieldToColumn(sortBy);
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, dbSortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Long appId = getCurrentAppId();
        
        Page<AppConfig> configs = appConfigRepository.findAllWithFiltersForContent(
            appId, group, dataType, required, search, pageable);

        Page<AppConfigContentResponse> response = configs.map(this::mapToContentResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
        summary = "Get all app configurations",
        description = "Retrieve all app configurations for the authenticated app. Supports filtering and sorting but returns complete list without pagination."
    )
    public ResponseEntity<List<AppConfigContentResponse>> getAllAppConfigs(
            @Parameter(description = "Filter by group name") @RequestParam(required = false) String group,
            @Parameter(description = "Filter by data type") @RequestParam(required = false) String dataType,
            @Parameter(description = "Filter by required status") @RequestParam(required = false) Boolean required,
            @Parameter(description = "Search in config key or description") @RequestParam(required = false) String search,
            @Parameter(description = "Sort field (configKey, group, dataType, createdAt)") @RequestParam(defaultValue = "configKey") String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(defaultValue = "asc") String sortDir) {

        // Create sort - map field names to database column names for native queries
        String dbSortField = mapSortFieldToColumn(sortBy);
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, dbSortField);

        Long appId = getCurrentAppId();
        
        // Use a large page size to get all results
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, sort);
        Page<AppConfig> configs = appConfigRepository.findAllWithFiltersForContent(
            appId, group, dataType, required, search, pageable);

        List<AppConfigContentResponse> response = configs.getContent().stream()
            .map(this::mapToContentResponse)
            .toList();
            
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{configKey}")
    @Operation(
        summary = "Get specific app configuration",
        description = "Retrieve a specific app configuration by config key"
    )
    public ResponseEntity<AppConfigContentResponse> getAppConfig(
            @Parameter(description = "Configuration key") @PathVariable String configKey) {

        Long appId = getCurrentAppId();
        
        AppConfig config = appConfigRepository.findByConfigKeyAndApp_Id(configKey, appId)
            .orElseThrow(() -> new RuntimeException("App configuration not found"));

        return ResponseEntity.ok(mapToContentResponse(config));
    }

    @GetMapping("/groups")
    @Operation(
        summary = "Get configuration groups",
        description = "Retrieve all distinct configuration groups for the authenticated app"
    )
    public ResponseEntity<List<String>> getGroups() {
        Long appId = getCurrentAppId();
        
        List<String> groups = appConfigRepository.findDistinctGroupsByApp_Id(appId);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/data-types")
    @Operation(
        summary = "Get data types",
        description = "Retrieve all distinct data types used in configurations"
    )
    public ResponseEntity<List<String>> getDataTypes() {
        Long appId = getCurrentAppId();
        
        List<String> dataTypes = appConfigRepository.findDistinctDataTypesByApp_Id(appId);
        return ResponseEntity.ok(dataTypes);
    }

    private AppConfigContentResponse mapToContentResponse(AppConfig config) {
        return AppConfigContentResponse.builder()
            .id(config.getId())
            .configKey(config.getConfigKey())
            .configValue(config.getConfigValue()) // Show actual value for content API
            .description(config.getDescription())
            .dataType(config.getConfigType() != null ? config.getConfigType().name() : null)
            .isRequired(config.getIsRequired())
            .isEncrypted(false) // AppConfig doesn't have encryption field
            .defaultValue(config.getDefaultValue())
            .validationRule(config.getValidationRules())
            .groupName(null) // Will need to implement group lookup if needed
            .groupDescription(null) // Will need to implement group lookup if needed
            .createdAt(config.getCreatedAt())
            .updatedAt(config.getUpdatedAt())
            .build();
    }

    private String mapSortFieldToColumn(String sortBy) {
        // Map API field names to database column names for native queries
        return switch (sortBy) {
            case "configKey" -> "config_key";
            case "configValue" -> "config_value";
            case "dataType" -> "config_type";
            case "isRequired" -> "is_required";
            case "defaultValue" -> "default_value";
            case "validationRule" -> "validation_rules";
            case "createdAt" -> "created_at";
            case "updatedAt" -> "updated_at";
            case "createdBy" -> "created_by";
            case "updatedBy" -> "updated_by";
            default -> sortBy; // For fields that match (description, etc.)
        };
    }

    private Long getCurrentAppId() {
        return (Long) org.springframework.web.context.request.RequestContextHolder
            .currentRequestAttributes()
            .getAttribute("appId", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
    }
}