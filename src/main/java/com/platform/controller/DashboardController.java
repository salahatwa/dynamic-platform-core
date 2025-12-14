package com.platform.controller;

import com.platform.dto.DashboardStatsDto;
import com.platform.dto.RecentActivityDto;
import com.platform.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get dashboard statistics
     * @param appId Optional app ID for app-specific stats
     * @return Dashboard statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats(@RequestParam(required = false) Long appId) {
        DashboardStatsDto stats = dashboardService.getDashboardStats(appId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get recent activity
     * @param appId Optional app ID for app-specific activity
     * @param size Number of items to return (default 10)
     * @return Recent activity list
     */
    @GetMapping("/activity")
    public ResponseEntity<Page<RecentActivityDto>> getRecentActivity(
            @RequestParam(required = false) Long appId,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(0, size, Sort.by("timestamp").descending());
        Page<RecentActivityDto> activity = dashboardService.getRecentActivity(appId, pageable);
        return ResponseEntity.ok(activity);
    }

    /**
     * Get audit logs (future implementation)
     * @param appId Optional app ID for app-specific logs
     * @param page Page number (default 0)
     * @param size Page size (default 20)
     * @return Audit logs
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<Page<RecentActivityDto>> getAuditLogs(
            @RequestParam(required = false) Long appId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<RecentActivityDto> auditLogs = dashboardService.getAuditLogs(appId, pageable);
        return ResponseEntity.ok(auditLogs);
    }
}