package com.platform.security;

import com.platform.enums.PermissionAction;
import com.platform.enums.PermissionResource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    
    /**
     * The resource for which permission is required
     */
    PermissionResource resource();
    
    /**
     * The action for which permission is required
     */
    PermissionAction action();
    
    /**
     * Optional message to return when permission is denied
     */
    String message() default "Access denied: insufficient permissions";
}