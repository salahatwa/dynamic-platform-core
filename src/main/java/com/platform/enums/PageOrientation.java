package com.platform.enums;

public enum PageOrientation {
    PORTRAIT("A4 Vertical (Portrait)", "Suitable for: Account statements, invoices, letters, reports"),
    LANDSCAPE("A4 Horizontal (Landscape)", "Suitable for: Certificates, diplomas, charts, wide tables");
    
    private final String displayName;
    private final String description;
    
    PageOrientation(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isLandscape() {
        return this == LANDSCAPE;
    }
    
    public boolean isPortrait() {
        return this == PORTRAIT;
    }
}