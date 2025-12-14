package com.platform.controller;

import com.platform.entity.TranslationApp;
import com.platform.service.TranslationAppService;
import com.platform.service.TranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/translations")
@RequiredArgsConstructor
@Tag(name = "Public Translation API", description = "Public API for fetching translations")
public class TranslationPublicApiController {
    
    private final TranslationAppService appService;
    private final TranslationService translationService;
    
    @GetMapping("/{language}")
    @Operation(summary = "Get translations for a language (i18n format)")
    public ResponseEntity<?> getTranslations(
            @PathVariable String language,
            @RequestHeader("X-API-Key") String apiKey) {
        
        TranslationApp app = appService.getByApiKey(apiKey)
            .orElseThrow(() -> new RuntimeException("Invalid API key"));
        
        if (!app.getActive()) {
            return ResponseEntity.status(403).body("App is inactive");
        }
        
        if (!app.getSupportedLanguagesSet().contains(language)) {
            return ResponseEntity.badRequest().body("Language not supported by this app");
        }
        
        Map<String, String> translations = translationService.getTranslationsAsMap(app.getId(), language);
        
        return ResponseEntity.ok(Map.of(
            "language", language,
            "translations", translations,
            "count", translations.size()
        ));
    }
    
    @GetMapping("/all")
    @Operation(summary = "Get all translations for all languages")
    public ResponseEntity<?> getAllTranslations(@RequestHeader("X-API-Key") String apiKey) {
        TranslationApp app = appService.getByApiKey(apiKey)
            .orElseThrow(() -> new RuntimeException("Invalid API key"));
        
        if (!app.getActive()) {
            return ResponseEntity.status(403).body("App is inactive");
        }
        
        Map<String, Map<String, String>> allTranslations = new java.util.HashMap<>();
        
        for (String language : app.getSupportedLanguagesSet()) {
            Map<String, String> translations = translationService.getTranslationsAsMap(app.getId(), language);
            allTranslations.put(language, translations);
        }
        
        return ResponseEntity.ok(Map.of(
            "app", app.getName(),
            "defaultLanguage", app.getDefaultLanguage(),
            "supportedLanguages", app.getSupportedLanguagesSet(),
            "translations", allTranslations
        ));
    }
}
