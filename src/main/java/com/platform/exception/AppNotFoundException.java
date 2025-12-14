package com.platform.exception;

public class AppNotFoundException extends RuntimeException {
    
    public AppNotFoundException(Long appId) {
        super("App not found with id: " + appId);
    }
    
    public AppNotFoundException(String appKey) {
        super("App not found with key: " + appKey);
    }
    
    public AppNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
