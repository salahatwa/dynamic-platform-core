package com.platform.controller.content;

import com.platform.dto.TranslationContentResponse;
import com.platform.entity.TranslationKey;
import com.platform.entity.Translation;
import com.platform.repository.TranslationKeyRepository;
import com.platform.repository.TranslationRepository;
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
@RequestMapping("/api/content/translations")
@RequiredArgsConstructor
@Tag(name = "Content API - Translations", description = "Public API for accessing translations with API key authentication")
@SecurityRequirement(name = "ApiKey")
public class ContentTranslationController {

    private final TranslationKeyRepository translationKeyRepository;
    private final TranslationRepository translationRepository;

    @GetMapping("/page")
    @Operation(
        summary = "Get translations for app",
        description = "Retrieve translation keys with all language values for the authenticated app. Supports filtering, sorting, and pagination."
    )
    public ResponseEntity<Page<TranslationContentResponse>> getTranslations(
            @Parameter(description = "Filter by category") @RequestParam(required = false) String category,
            @Parameter(description = "Search in key name or default value") @RequestParam(required = false) String search,
            @Parameter(description = "Sort field (keyName, category, createdAt, updatedAt)") @RequestParam(defaultValue = "keyName") String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Long appId = getCurrentAppId();
        
        Page<TranslationKey> translationKeys;
        
        if (category != null && search != null) {
            translationKeys = translationKeyRepository.findByApp_IdAndContextContainingIgnoreCaseAndKeyNameContainingIgnoreCase(appId, category, search, pageable);
        } else if (category != null) {
            translationKeys = translationKeyRepository.findByApp_IdAndContextContainingIgnoreCase(appId, category, pageable);
        } else if (search != null) {
            translationKeys = translationKeyRepository.findByApp_IdAndKeyNameContainingIgnoreCase(appId, search, pageable);
        } else {
            translationKeys = translationKeyRepository.findByApp_Id(appId, pageable);
        }

        Page<TranslationContentResponse> response = translationKeys.map(this::mapToContentResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
        summary = "Get all translations for app",
        description = "Retrieve all translation keys with all language values for the authenticated app. Supports filtering and sorting but returns complete list without pagination."
    )
    public ResponseEntity<List<TranslationContentResponse>> getAllTranslations(
            @Parameter(description = "Filter by category") @RequestParam(required = false) String category,
            @Parameter(description = "Search in key name or default value") @RequestParam(required = false) String search,
            @Parameter(description = "Sort field (keyName, category, createdAt, updatedAt)") @RequestParam(defaultValue = "keyName") String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);

        Long appId = getCurrentAppId();
        
        // Use a large page size to get all results, then extract content
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, sort);
        Page<TranslationKey> translationKeysPage;
        
        if (category != null && search != null) {
            translationKeysPage = translationKeyRepository.findByApp_IdAndContextContainingIgnoreCaseAndKeyNameContainingIgnoreCase(appId, category, search, pageable);
        } else if (category != null) {
            translationKeysPage = translationKeyRepository.findByApp_IdAndContextContainingIgnoreCase(appId, category, pageable);
        } else if (search != null) {
            translationKeysPage = translationKeyRepository.findByApp_IdAndKeyNameContainingIgnoreCase(appId, search, pageable);
        } else {
            translationKeysPage = translationKeyRepository.findByApp_Id(appId, pageable);
        }

        List<TranslationContentResponse> response = translationKeysPage.getContent().stream()
            .map(this::mapToContentResponse)
            .toList();
            
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{keyName}")
    @Operation(
        summary = "Get specific translation key",
        description = "Retrieve a specific translation key with all language values by key name"
    )
    public ResponseEntity<TranslationContentResponse> getTranslation(
            @Parameter(description = "Translation key name") @PathVariable String keyName) {

        Long appId = getCurrentAppId();
        
        TranslationKey translationKey = translationKeyRepository.findByKeyNameAndApp_Id(keyName, appId)
            .orElseThrow(() -> new RuntimeException("Translation key not found"));

        return ResponseEntity.ok(mapToContentResponse(translationKey));
    }

    @GetMapping("/categories")
    @Operation(
        summary = "Get translation categories",
        description = "Retrieve all distinct categories for the authenticated app"
    )
    public ResponseEntity<List<String>> getCategories() {
        Long appId = getCurrentAppId();
        
        List<String> categories = translationKeyRepository.findDistinctCategoriesByApp_Id(appId);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/languages")
    @Operation(
        summary = "Get supported languages",
        description = "Retrieve all languages that have translations for the authenticated app"
    )
    public ResponseEntity<List<String>> getLanguages() {
        Long appId = getCurrentAppId();
        
        List<String> languages = translationRepository.findDistinctLanguagesByApp_Id(appId);
        return ResponseEntity.ok(languages);
    }

    private TranslationContentResponse mapToContentResponse(TranslationKey translationKey) {
        // Get all translation values for this key
        List<Translation> values = translationRepository.findByKeyId(translationKey.getId());
        
        Map<String, String> translationMap = values.stream()
            .collect(Collectors.toMap(
                Translation::getLanguage,
                Translation::getValue
            ));

        return TranslationContentResponse.builder()
            .id(translationKey.getId())
            .keyName(translationKey.getKeyName())
            .defaultValue(null) // TranslationKey doesn't have defaultValue
            .description(translationKey.getDescription())
            .category(translationKey.getContext()) // Using context as category
            .translations(translationMap)
            .createdAt(translationKey.getCreatedAt())
            .updatedAt(translationKey.getUpdatedAt())
            .build();
    }

    private Long getCurrentAppId() {
        return (Long) org.springframework.web.context.request.RequestContextHolder
            .currentRequestAttributes()
            .getAttribute("appId", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
    }
}