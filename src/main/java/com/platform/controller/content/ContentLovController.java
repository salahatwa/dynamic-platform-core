package com.platform.controller.content;

import com.platform.dto.LovContentResponse;
import com.platform.entity.Lov;
import com.platform.repository.LovRepository;
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
@RequestMapping("/api/content/lov")
@RequiredArgsConstructor
@Tag(name = "Content API - List of Values", description = "Public API for accessing LOV with API key authentication")
@SecurityRequirement(name = "ApiKey")
public class ContentLovController {

    private final LovRepository lovRepository;

    @GetMapping("/page")
    @Operation(
        summary = "Get Page LOVs for app",
        description = "Retrieve Page of Values for the authenticated app. Supports filtering, sorting, and pagination."
    )
    public ResponseEntity<Page<LovContentResponse>> getPageLovs(
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Search in LOV name or description") @RequestParam(required = false) String search,
            @Parameter(description = "Sort field (lovName, lovCode, lovType, displayOrder, createdAt, updatedAt)") @RequestParam(defaultValue = "lovName") String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        // Map API field names to entity property names
        String entitySortField = mapSortFieldToEntityProperty(sortBy);
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, entitySortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Long appId = getCurrentAppId();
        
        Page<Lov> lovs;
        
        if (active != null && search != null) {
            lovs = lovRepository.findByApp_IdAndActiveAndLovCodeContainingIgnoreCase(appId, active, search, pageable);
        } else if (active != null) {
            lovs = lovRepository.findByApp_IdAndActive(appId, active, pageable);
        } else if (search != null) {
            lovs = lovRepository.findByApp_IdAndLovCodeContainingIgnoreCase(appId, search, pageable);
        } else {
            lovs = lovRepository.findByApp_Id(appId, pageable);
        }

        Page<LovContentResponse> response = lovs.map(this::mapToContentResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
        summary = "Get all LOVs for app",
        description = "Retrieve all List of Values for the authenticated app. Supports filtering and sorting but returns complete list without pagination."
    )
    public ResponseEntity<List<LovContentResponse>> getAllLovs(
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Search in LOV name or description") @RequestParam(required = false) String search,
            @Parameter(description = "Sort field (lovName, lovCode, lovType, displayOrder, createdAt, updatedAt)") @RequestParam(defaultValue = "lovName") String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(defaultValue = "asc") String sortDir) {

        // Map API field names to entity property names
        String entitySortField = mapSortFieldToEntityProperty(sortBy);
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, entitySortField);

        Long appId = getCurrentAppId();
        
        // Use a large page size to get all results, then extract content
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, sort);
        Page<Lov> lovsPage;
        
        if (active != null && search != null) {
            lovsPage = lovRepository.findByApp_IdAndActiveAndLovCodeContainingIgnoreCase(appId, active, search, pageable);
        } else if (active != null) {
            lovsPage = lovRepository.findByApp_IdAndActive(appId, active, pageable);
        } else if (search != null) {
            lovsPage = lovRepository.findByApp_IdAndLovCodeContainingIgnoreCase(appId, search, pageable);
        } else {
            lovsPage = lovRepository.findByApp_Id(appId, pageable);
        }
        
        List<Lov> lovs = lovsPage.getContent();

        List<LovContentResponse> response = lovs.stream()
            .map(this::mapToContentResponse)
            .toList();
            
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{lovCode}")
    @Operation(
        summary = "Get specific LOV",
        description = "Retrieve a specific LOV by LOV code"
    )
    public ResponseEntity<LovContentResponse> getLov(
            @Parameter(description = "LOV code") @PathVariable String lovCode) {

        Long appId = getCurrentAppId();
        
        Lov lov = lovRepository.findByLovCodeAndApp_Id(lovCode, appId)
            .orElseThrow(() -> new RuntimeException("LOV not found"));

        return ResponseEntity.ok(mapToContentResponse(lov));
    }

    @GetMapping("/types")
    @Operation(
        summary = "Get LOV types",
        description = "Retrieve all distinct LOV types for the authenticated app"
    )
    public ResponseEntity<List<String>> getLovTypes() {
        Long appId = getCurrentAppId();
        
        List<String> types = lovRepository.findDistinctLovTypesByApp_Id(appId);
        return ResponseEntity.ok(types);
    }

    private LovContentResponse mapToContentResponse(Lov lov) {
        // Create a single value data from the LOV entity
        LovContentResponse.LovValueData valueData = LovContentResponse.LovValueData.builder()
            .id(lov.getId())
            .value(lov.getLovValue())
            .displayValue(lov.getLovValue()) // Using lovValue as display value
            .description(lov.getAttribute1()) // Using attribute1 as description
            .sortOrder(lov.getDisplayOrder())
            .isActive(lov.getActive())
            .build();

        return LovContentResponse.builder()
            .id(lov.getId())
            .lovName(lov.getLovCode()) // Map lovCode to lovName for API response
            .description(lov.getAttribute2()) // Using attribute2 as description
            .isActive(lov.getActive())
            .values(List.of(valueData)) // Single value in list
            .createdAt(lov.getCreatedAt())
            .updatedAt(lov.getUpdatedAt())
            .build();
    }

    private String mapSortFieldToEntityProperty(String sortBy) {
        // Map API field names to entity property names for JPA queries
        return switch (sortBy) {
            case "lovName" -> "lovCode"; // Map lovName to lovCode entity property
            case "lovCode" -> "lovCode";
            case "lovType" -> "lovType";
            case "displayOrder" -> "displayOrder";
            case "createdAt" -> "createdAt";
            case "updatedAt" -> "updatedAt";
            default -> "lovCode"; // Default to lovCode
        };
    }

    private Long getCurrentAppId() {
        return (Long) org.springframework.web.context.request.RequestContextHolder
            .currentRequestAttributes()
            .getAttribute("appId", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
    }
}