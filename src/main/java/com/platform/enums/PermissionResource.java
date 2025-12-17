package com.platform.enums;

public enum PermissionResource {
    TRANSLATIONS("translations", "Translation Management"),
    TEMPLATES("templates", "Template Management"),
    LOV("lov", "List of Values Management"),
    APP_CONFIG("app_config", "App Configuration Management"),
    ERROR_CODES("error_codes", "Error Code Management"),
    USERS("users", "User Management"),
    ROLES("roles", "Role Management"),
    INVITATIONS("invitations", "Invitation Management"),
    APPS("apps", "Application Management"),
    DASHBOARD("dashboard", "Dashboard Access"),
    API_KEYS("api_keys", "API Key Management"),
    MEDIA("media", "Media Management"),
    ORGANIZATION("organization", "Organization Management");
    
    private final String resource;
    private final String description;
    
    PermissionResource(String resource, String description) {
        this.resource = resource;
        this.description = description;
    }
    
    public String getResource() {
        return resource;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return resource;
    }
}