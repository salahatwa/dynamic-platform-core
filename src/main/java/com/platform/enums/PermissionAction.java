package com.platform.enums;

public enum PermissionAction {
    CREATE("create", "Create new records"),
    READ("read", "View/inquiry records"),
    UPDATE("update", "Modify existing records"),
    DELETE("delete", "Remove records");
    
    private final String action;
    private final String description;
    
    PermissionAction(String action, String description) {
        this.action = action;
        this.description = description;
    }
    
    public String getAction() {
        return action;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return action;
    }
}