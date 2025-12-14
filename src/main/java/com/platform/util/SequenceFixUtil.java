package com.platform.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Utility class to fix database sequence synchronization issues
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SequenceFixUtil {
    
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * Fix sequence for a specific table
     */
    public void fixSequence(String tableName, String sequenceName, String idColumn) {
        try {
            log.info("Fixing sequence {} for table {}", sequenceName, tableName);
            
            // Get current max ID from table
            String maxIdQuery = String.format("SELECT COALESCE(MAX(%s), 0) FROM %s", idColumn, tableName);
            Long maxId = jdbcTemplate.queryForObject(maxIdQuery, Long.class);
            
            log.info("Current max ID in {}: {}", tableName, maxId);
            
            // Set sequence to max ID + 1
            Long nextSeqValue = maxId + 1;
            String fixQuery = String.format("SELECT setval('%s', %d, false)", sequenceName, nextSeqValue);
            jdbcTemplate.execute(fixQuery);
            
            log.info("Fixed sequence {} - next value will be: {}", sequenceName, nextSeqValue);
            
            // Verify the fix
            String verifyQuery = String.format("SELECT currval('%s')", sequenceName);
            Long currentSeqValue = jdbcTemplate.queryForObject(verifyQuery, Long.class);
            
            log.info("Verified: sequence {} current value is {}", sequenceName, currentSeqValue);
            
        } catch (Exception e) {
            log.error("Failed to fix sequence {} for table {}: {}", sequenceName, tableName, e.getMessage(), e);
            throw new RuntimeException("Failed to fix sequence: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fix apps table sequence specifically
     */
    public void fixAppsSequence() {
        fixSequence("apps", "apps_id_seq", "id");
    }
    
    /**
     * Check if sequence is out of sync
     */
    public boolean isSequenceOutOfSync(String tableName, String sequenceName, String idColumn) {
        try {
            // Get max ID from table
            String maxIdQuery = String.format("SELECT COALESCE(MAX(%s), 0) FROM %s", idColumn, tableName);
            Long maxId = jdbcTemplate.queryForObject(maxIdQuery, Long.class);
            
            // Get next sequence value (without consuming it)
            String nextSeqQuery = String.format("SELECT nextval('%s')", sequenceName);
            Long nextSeqValue = jdbcTemplate.queryForObject(nextSeqQuery, Long.class);
            
            // Reset sequence back since we just consumed a value
            String resetQuery = String.format("SELECT setval('%s', %d, false)", sequenceName, nextSeqValue);
            jdbcTemplate.execute(resetQuery);
            
            // Check if there's a conflict
            boolean outOfSync = nextSeqValue <= maxId;
            
            if (outOfSync) {
                log.warn("Sequence {} is out of sync! Next value: {}, Max existing ID: {}", 
                    sequenceName, nextSeqValue, maxId);
            } else {
                log.info("Sequence {} is in sync. Next value: {}, Max existing ID: {}", 
                    sequenceName, nextSeqValue, maxId);
            }
            
            return outOfSync;
            
        } catch (Exception e) {
            log.error("Failed to check sequence sync for {}: {}", sequenceName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check all common sequences
     */
    public void checkAllSequences() {
        log.info("Checking all sequences for synchronization issues...");
        
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables " +
            "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'"
        );
        
        for (Map<String, Object> table : tables) {
            String tableName = (String) table.get("table_name");
            String sequenceName = tableName + "_id_seq";
            
            // Check if sequence exists
            List<Map<String, Object>> sequences = jdbcTemplate.queryForList(
                "SELECT sequence_name FROM information_schema.sequences WHERE sequence_name = ?",
                sequenceName
            );
            
            if (!sequences.isEmpty()) {
                log.info("Checking sequence for table: {}", tableName);
                if (isSequenceOutOfSync(tableName, sequenceName, "id")) {
                    log.warn("Found out-of-sync sequence: {} for table: {}", sequenceName, tableName);
                }
            }
        }
    }
    
    /**
     * Auto-fix all out-of-sync sequences
     */
    public void autoFixAllSequences() {
        log.info("Auto-fixing all out-of-sync sequences...");
        
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables " +
            "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'"
        );
        
        int fixedCount = 0;
        
        for (Map<String, Object> table : tables) {
            String tableName = (String) table.get("table_name");
            String sequenceName = tableName + "_id_seq";
            
            // Check if sequence exists
            List<Map<String, Object>> sequences = jdbcTemplate.queryForList(
                "SELECT sequence_name FROM information_schema.sequences WHERE sequence_name = ?",
                sequenceName
            );
            
            if (!sequences.isEmpty()) {
                if (isSequenceOutOfSync(tableName, sequenceName, "id")) {
                    log.info("Auto-fixing sequence: {} for table: {}", sequenceName, tableName);
                    fixSequence(tableName, sequenceName, "id");
                    fixedCount++;
                }
            }
        }
        
        log.info("Auto-fix complete. Fixed {} sequences.", fixedCount);
    }
}