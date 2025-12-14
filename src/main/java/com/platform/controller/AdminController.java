package com.platform.controller;

import com.platform.util.SequenceFixUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin controller for system maintenance operations
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final SequenceFixUtil sequenceFixUtil;
    
    /**
     * Fix apps table sequence
     */
    @PostMapping("/fix-apps-sequence")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> fixAppsSequence() {
        log.info("Admin requested apps sequence fix");
        
        try {
            sequenceFixUtil.fixAppsSequence();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Apps sequence fixed successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to fix apps sequence: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fix apps sequence: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Check all sequences for synchronization issues
     */
    @GetMapping("/check-sequences")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkSequences() {
        log.info("Admin requested sequence check");
        
        try {
            sequenceFixUtil.checkAllSequences();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sequence check completed - see logs for details");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to check sequences: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to check sequences: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Auto-fix all out-of-sync sequences
     */
    @PostMapping("/fix-all-sequences")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> fixAllSequences() {
        log.info("Admin requested auto-fix for all sequences");
        
        try {
            sequenceFixUtil.autoFixAllSequences();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All sequences auto-fixed successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to auto-fix sequences: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to auto-fix sequences: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Check if apps sequence is out of sync
     */
    @GetMapping("/check-apps-sequence")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkAppsSequence() {
        log.info("Admin requested apps sequence check");
        
        try {
            boolean outOfSync = sequenceFixUtil.isSequenceOutOfSync("apps", "apps_id_seq", "id");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("outOfSync", outOfSync);
            response.put("message", outOfSync ? 
                "Apps sequence is OUT OF SYNC - needs fixing" : 
                "Apps sequence is synchronized");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to check apps sequence: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to check apps sequence: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}