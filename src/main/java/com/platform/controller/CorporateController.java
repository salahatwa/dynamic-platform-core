package com.platform.controller;

import com.platform.entity.Corporate;
import com.platform.service.CorporateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/corporate")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CorporateController {

    private final CorporateService corporateService;

    /**
     * Get current corporate information
     */
    @GetMapping
    public ResponseEntity<Corporate> getCurrentCorporate() {
        try {
            Corporate corporate = corporateService.getCurrentCorporate();
            return ResponseEntity.ok(corporate);
        } catch (Exception e) {
            log.error("Error getting current corporate: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update corporate information
     */
    @PutMapping
    public ResponseEntity<Corporate> updateCorporate(@Valid @RequestBody Corporate corporate) {
        try {
            Corporate updatedCorporate = corporateService.updateCorporate(corporate);
            return ResponseEntity.ok(updatedCorporate);
        } catch (RuntimeException e) {
            log.error("Error updating corporate: ", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error updating corporate: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check if corporate name is available
     */
    @GetMapping("/check-name")
    public ResponseEntity<Map<String, Boolean>> checkNameAvailability(@RequestParam String name) {
        try {
            boolean available = corporateService.isNameAvailable(name);
            return ResponseEntity.ok(Map.of("available", available));
        } catch (Exception e) {
            log.error("Error checking name availability: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Check if corporate domain is available
     */
    @GetMapping("/check-domain")
    public ResponseEntity<Map<String, Boolean>> checkDomainAvailability(@RequestParam String domain) {
        try {
            boolean available = corporateService.isDomainAvailable(domain);
            return ResponseEntity.ok(Map.of("available", available));
        } catch (Exception e) {
            log.error("Error checking domain availability: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get corporate by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Corporate> getCorporateById(@PathVariable Long id) {
        try {
            Corporate corporate = corporateService.getCorporateById(id);
            return ResponseEntity.ok(corporate);
        } catch (RuntimeException e) {
            log.error("Error getting corporate by ID: ", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error getting corporate by ID: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}