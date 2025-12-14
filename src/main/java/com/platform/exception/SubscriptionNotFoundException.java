package com.platform.exception;

public class SubscriptionNotFoundException extends RuntimeException {
    
    public SubscriptionNotFoundException(Long corporateId) {
        super("No active subscription found for corporate: " + corporateId);
    }
    
    public SubscriptionNotFoundException(String message) {
        super(message);
    }
}
