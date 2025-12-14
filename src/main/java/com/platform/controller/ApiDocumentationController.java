package com.platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/documentation")
@Tag(name = "API Documentation", description = "API usage documentation and guides")
public class ApiDocumentationController {

    @GetMapping("/api-keys")
    @Operation(
        summary = "Get API Keys usage guide",
        description = "Retrieve comprehensive guide on how to use API keys to access content APIs"
    )
    public ResponseEntity<Map<String, Object>> getApiKeysGuide() {
        
        Map<String, Object> guide = Map.of(
            "title", "API Keys Usage Guide",
            "description", "Learn how to use API keys to access your app's content through our REST APIs",
            
            "authentication", Map.of(
                "method", "Bearer Token",
                "header", "Authorization: Bearer tms_your_key_here",
                "format", "All API keys start with 'tms_' prefix",
                "example", "Authorization: Bearer tms_abc123def456ghi789"
            ),
            
            "baseUrl", "http://localhost:8080/api/content",
            
            "endpoints", Map.of(
                "errorCodes", Map.of(
                    "url", "/api/content/error-codes",
                    "description", "Access error codes with all translations",
                    "methods", Map.of(
                        "GET /api/content/error-codes", "List all error codes with filtering and pagination",
                        "GET /api/content/error-codes/{errorCode}", "Get specific error code by code",
                        "GET /api/content/error-codes/categories", "Get all error code categories",
                        "GET /api/content/error-codes/modules", "Get all error code modules"
                    ),
                    "filters", Map.of(
                        "categoryId", "Filter by category ID",
                        "severity", "Filter by severity (INFO, WARNING, ERROR, CRITICAL)",
                        "status", "Filter by status (ACTIVE, DEPRECATED, REMOVED)",
                        "module", "Filter by module name",
                        "search", "Search in error code or message"
                    )
                ),
                
                "templates", Map.of(
                    "url", "/api/content/templates",
                    "description", "Access email and document templates",
                    "methods", Map.of(
                        "GET /api/content/templates", "List all templates with filtering and pagination",
                        "GET /api/content/templates/{id}", "Get specific template by ID",
                        "GET /api/content/templates/types", "Get all template types"
                    ),
                    "filters", Map.of(
                        "type", "Filter by template type (EMAIL, PDF, HTML, WORD)",
                        "search", "Search in template name"
                    )
                ),
                
                "translations", Map.of(
                    "url", "/api/content/translations",
                    "description", "Access translation keys and values for all languages",
                    "methods", Map.of(
                        "GET /api/content/translations", "List all translation keys with all language values",
                        "GET /api/content/translations/{keyName}", "Get specific translation key with all languages",
                        "GET /api/content/translations/categories", "Get all translation categories",
                        "GET /api/content/translations/languages", "Get all supported languages"
                    ),
                    "filters", Map.of(
                        "category", "Filter by category",
                        "search", "Search in key name or default value"
                    )
                ),
                
                "lov", Map.of(
                    "url", "/api/content/lov",
                    "description", "Access List of Values (LOV) data",
                    "methods", Map.of(
                        "GET /api/content/lov", "List all LOVs with filtering and pagination",
                        "GET /api/content/lov/{lovCode}", "Get specific LOV by code",
                        "GET /api/content/lov/types", "Get all LOV types"
                    ),
                    "filters", Map.of(
                        "active", "Filter by active status (true/false)",
                        "search", "Search in LOV code"
                    )
                ),
                
                "appConfig", Map.of(
                    "url", "/api/content/app-config",
                    "description", "Access application configuration settings",
                    "methods", Map.of(
                        "GET /api/content/app-config", "List all app configurations with filtering and pagination",
                        "GET /api/content/app-config/{configKey}", "Get specific configuration by key",
                        "GET /api/content/app-config/groups", "Get all configuration groups",
                        "GET /api/content/app-config/data-types", "Get all data types"
                    ),
                    "filters", Map.of(
                        "group", "Filter by group name",
                        "dataType", "Filter by data type",
                        "required", "Filter by required status (true/false)",
                        "search", "Search in config key or description"
                    )
                )
            ),
            
            "commonParameters", Map.of(
                "pagination", Map.of(
                    "page", "Page number (0-based, default: 0)",
                    "size", "Page size (default: 20)"
                ),
                "sorting", Map.of(
                    "sortBy", "Field to sort by (varies by endpoint)",
                    "sortDir", "Sort direction: 'asc' or 'desc' (default: 'asc')"
                )
            ),
            
            "examples", Map.of(
                "basicRequest", Map.of(
                    "description", "Basic request to get error codes",
                    "curl", "curl -H \"Authorization: Bearer tms_your_key\" \"http://localhost:8080/api/content/error-codes\"",
                    "javascript", """
                        fetch('http://localhost:8080/api/content/error-codes', {
                          headers: {
                            'Authorization': 'Bearer tms_your_key'
                          }
                        })
                        .then(response => response.json())
                        .then(data => console.log(data));
                        """
                ),
                
                "filteredRequest", Map.of(
                    "description", "Filtered request with pagination and search",
                    "curl", "curl -H \"Authorization: Bearer tms_your_key\" \"http://localhost:8080/api/content/error-codes?severity=ERROR&search=validation&page=0&size=10&sortBy=errorCode&sortDir=asc\"",
                    "javascript", """
                        const params = new URLSearchParams({
                          severity: 'ERROR',
                          search: 'validation',
                          page: '0',
                          size: '10',
                          sortBy: 'errorCode',
                          sortDir: 'asc'
                        });
                        
                        fetch(`http://localhost:8080/api/content/error-codes?${params}`, {
                          headers: {
                            'Authorization': 'Bearer tms_your_key'
                          }
                        })
                        .then(response => response.json())
                        .then(data => console.log(data));
                        """
                ),
                
                "translationsRequest", Map.of(
                    "description", "Get translations for a specific key",
                    "curl", "curl -H \"Authorization: Bearer tms_your_key\" \"http://localhost:8080/api/content/translations/welcome.message\"",
                    "response", Map.of(
                        "id", 1,
                        "keyName", "welcome.message",
                        "defaultValue", "Welcome to our platform",
                        "translations", Map.of(
                            "en", "Welcome to our platform",
                            "es", "Bienvenido a nuestra plataforma",
                            "fr", "Bienvenue sur notre plateforme"
                        )
                    )
                )
            ),
            
            "errorHandling", Map.of(
                "401", "Unauthorized - Invalid or missing API key",
                "403", "Forbidden - API key doesn't have access to this app",
                "404", "Not Found - Resource doesn't exist",
                "500", "Internal Server Error - Contact support"
            ),
            
            "bestPractices", java.util.List.of(
                "Store API keys securely and never commit them to version control",
                "Use environment variables or secure configuration management",
                "Implement proper error handling for API responses",
                "Use pagination for large datasets to improve performance",
                "Cache frequently accessed data to reduce API calls",
                "Monitor API key usage and set up alerts for unusual activity",
                "Rotate API keys regularly for security",
                "Use specific filters to reduce response payload size"
            ),
            
            "rateLimits", Map.of(
                "description", "API rate limits are applied per API key",
                "limits", "1000 requests per hour per API key",
                "headers", Map.of(
                    "X-RateLimit-Limit", "Total requests allowed per hour",
                    "X-RateLimit-Remaining", "Remaining requests in current window",
                    "X-RateLimit-Reset", "Time when the rate limit resets"
                )
            )
        );
        
        return ResponseEntity.ok(guide);
    }
}