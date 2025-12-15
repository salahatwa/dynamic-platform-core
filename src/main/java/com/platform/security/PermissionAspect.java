package com.platform.security;

import com.platform.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionAspect {
    
    private final PermissionService permissionService;
    
    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        
        boolean hasPermission = permissionService.hasPermission(
            requirePermission.resource(),
            requirePermission.action()
        );
        
        if (!hasPermission) {
            log.warn("Permission denied for resource: {} action: {}", 
                requirePermission.resource(), requirePermission.action());
            throw new AccessDeniedException(requirePermission.message());
        }
        
        return joinPoint.proceed();
    }
    
    @Around("@within(requirePermission)")
    public Object checkClassPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        
        boolean hasPermission = permissionService.hasPermission(
            requirePermission.resource(),
            requirePermission.action()
        );
        
        if (!hasPermission) {
            log.warn("Permission denied for class-level resource: {} action: {}", 
                requirePermission.resource(), requirePermission.action());
            throw new AccessDeniedException(requirePermission.message());
        }
        
        return joinPoint.proceed();
    }
}