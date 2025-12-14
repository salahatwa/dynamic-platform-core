package com.platform.exception;

public class AppLimitExceededException extends RuntimeException {
    
    private final int currentCount;
    private final int maxAllowed;
    private final String tier;
    
    public AppLimitExceededException(int currentCount, int maxAllowed, String tier) {
        super(String.format("App limit exceeded. You have %d apps but your %s plan allows only %d apps. Please upgrade your subscription.", 
            currentCount, tier, maxAllowed));
        this.currentCount = currentCount;
        this.maxAllowed = maxAllowed;
        this.tier = tier;
    }
    
    public int getCurrentCount() {
        return currentCount;
    }
    
    public int getMaxAllowed() {
        return maxAllowed;
    }
    
    public String getTier() {
        return tier;
    }
}
