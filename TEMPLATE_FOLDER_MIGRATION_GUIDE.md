# Template Folder Management System - Migration Guide

## Overview

This guide documents the database schema and migration setup for the Template Folder Management System, which provides a Google Drive-like hierarchical folder structure for organizing templates within corporate and application boundaries.

## Database Schema Changes

### 1. Enhanced Template Folders Table

The `template_folders` table has been enhanced with the following structure:

```sql
CREATE TABLE template_folders (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_id BIGINT NULL,
    application_id BIGINT NOT NULL,
    corporate_id BIGINT NOT NULL,
    path VARCHAR(1000) NOT NULL,           -- Materialized path for efficient queries
    level INTEGER NOT NULL DEFAULT 0,      -- Depth level in hierarchy
    sort_order INTEGER NOT NULL DEFAULT 0, -- Order within parent folder
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    
    FOREIGN KEY (parent_id) REFERENCES template_folders(id) ON DELETE CASCADE,
    FOREIGN KEY (application_id) REFERENCES apps(id) ON DELETE CASCADE,
    FOREIGN KEY (corporate_id) REFERENCES corporates(id) ON DELETE CASCADE,
    
    CONSTRAINT uk_name_parent_app UNIQUE (name, parent_id, application_id)
);
```

### 2. Folder Permissions Table

New table for role-based access control:

```sql
CREATE TABLE folder_permissions (
    id BIGSERIAL PRIMARY KEY,
    folder_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    can_view BOOLEAN DEFAULT TRUE,
    can_create BOOLEAN DEFAULT FALSE,
    can_edit BOOLEAN DEFAULT FALSE,
    can_delete BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (folder_id) REFERENCES template_folders(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    
    CONSTRAINT uk_folder_role UNIQUE (folder_id, role_id)
);
```

### 3. Templates Table Enhancement

The existing `templates` table has been enhanced with:
- `folder_id` column (already exists)
- Foreign key constraint to `template_folders`

## Migration Process

### Automatic Migration

The system includes automatic migration that runs on application startup:

1. **TemplateFolderInitializationService**: Runs the migration process automatically
2. **TemplateFolderMigrationService**: Handles the actual migration logic

### Migration Steps

1. **Create Root Folders**: For each application that doesn't have a root folder, create one named "Root"
2. **Move Templates**: Move all templates without valid `folder_id` to their application's root folder
3. **Set Permissions**: Create default permissions for admin and user roles
4. **Validate**: Verify migration success and report any issues

### Manual Migration

You can also run the migration manually using the REST API:

```bash
# Run migration
POST /api/admin/template-folder-migration/migrate

# Validate migration
GET /api/admin/template-folder-migration/validate
```

## Entity Classes

### TemplateFolder Entity

```java
@Entity
@Table(name = "template_folders")
public class TemplateFolder extends BaseEntity {
    private String name;
    private TemplateFolder parent;
    private App application;
    private Corporate corporate;
    private String path;
    private Integer level;
    private Integer sortOrder;
    private List<TemplateFolder> children;
    private List<Template> templates;
    private List<FolderPermission> permissions;
}
```

### FolderPermission Entity

```java
@Entity
@Table(name = "folder_permissions")
public class FolderPermission extends BaseEntity {
    private TemplateFolder folder;
    private Role role;
    private Boolean canView;
    private Boolean canCreate;
    private Boolean canEdit;
    private Boolean canDelete;
}
```

## Repository Interfaces

### TemplateFolderRepository

Enhanced with application-scoped queries:

```java
// Primary methods (application-scoped)
List<TemplateFolder> findByApplicationIdAndParentIsNullOrderBySortOrder(Long applicationId);
List<TemplateFolder> findByApplicationIdAndParentIdOrderBySortOrder(Long applicationId, Long parentId);
Optional<TemplateFolder> findByIdAndApplicationId(Long id, Long applicationId);

// Hierarchical queries
List<TemplateFolder> findAllDescendantsByPathAndApplicationId(String parentPath, Long applicationId);
List<TemplateFolder> findByLevelAndApplicationIdOrderBySortOrder(Integer level, Long applicationId);

// Statistics
long countTemplatesInFolder(Long folderId);
long countSubfoldersInFolder(Long folderId);
```

### FolderPermissionRepository

```java
// Permission management
Optional<FolderPermission> findByFolderIdAndRoleId(Long folderId, Long roleId);
List<FolderPermission> findByFolderId(Long folderId);
List<FolderPermission> findByRoleId(Long roleId);

// User-based queries
List<FolderPermission> findByFolderIdAndUserId(Long folderId, Long userId);
List<FolderPermission> findByUserIdAndPermissions(Long userId, Boolean canView, Boolean canCreate, Boolean canEdit, Boolean canDelete);
```

## Database Functions and Triggers

### Path Management

Automatic path calculation using triggers:

```sql
CREATE OR REPLACE FUNCTION update_folder_path()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.parent_id IS NOT NULL THEN
        SELECT path, level INTO parent_path, parent_level
        FROM template_folders WHERE id = NEW.parent_id;
        NEW.path := parent_path || '/' || NEW.id::text;
        NEW.level := parent_level + 1;
    ELSE
        NEW.path := '/' || NEW.id::text;
        NEW.level := 0;
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';
```

### Recursive Path Updates

Function to update all child folder paths when parent changes:

```sql
CREATE OR REPLACE FUNCTION update_child_folder_paths(folder_id BIGINT)
RETURNS VOID AS $$
-- Recursively updates paths of all descendant folders
```

## Validation and Monitoring

### Migration Validation

The system provides comprehensive validation:

```java
public class MigrationValidationResult {
    private long totalFolders;
    private long totalTemplates;
    private long totalPermissions;
    private long orphanedTemplates;      // Templates without valid folder_id
    private long foldersWithoutPath;     // Folders with invalid paths
    private boolean isValid;             // Overall validation status
}
```

### Helpful Views

Database views for easier querying:

```sql
-- Complete folder tree with statistics
CREATE VIEW folder_tree_view AS
SELECT tf.*, a.name as application_name, c.name as corporate_name,
       (SELECT COUNT(*) FROM templates t WHERE t.folder_id = tf.id) as template_count,
       (SELECT COUNT(*) FROM template_folders child WHERE child.parent_id = tf.id) as subfolder_count
FROM template_folders tf
JOIN apps a ON tf.application_id = a.id
JOIN corporates c ON tf.corporate_id = c.id;

-- Templates with folder information
CREATE VIEW templates_with_folder_view AS
SELECT t.*, tf.name as folder_name, tf.path as folder_path, tf.level as folder_level
FROM templates t
LEFT JOIN template_folders tf ON t.folder_id = tf.id;
```

## Troubleshooting

### Common Issues

1. **Orphaned Templates**: Templates with `folder_id` pointing to non-existent folders
   - Solution: Run migration to move them to root folders

2. **Invalid Paths**: Folders with NULL or empty path values
   - Solution: Update paths using the trigger function

3. **Permission Issues**: Missing default permissions for folders
   - Solution: Run permission creation for affected folders

### Monitoring Queries

```sql
-- Check for orphaned templates
SELECT COUNT(*) FROM templates t 
WHERE t.folder_id IS NOT NULL 
AND NOT EXISTS (SELECT 1 FROM template_folders tf WHERE tf.id = t.folder_id);

-- Check for folders without proper paths
SELECT COUNT(*) FROM template_folders WHERE path IS NULL OR path = '';

-- Check permission coverage
SELECT tf.id, tf.name, COUNT(fp.id) as permission_count
FROM template_folders tf
LEFT JOIN folder_permissions fp ON tf.id = fp.folder_id
GROUP BY tf.id, tf.name
HAVING COUNT(fp.id) = 0;
```

## Performance Considerations

1. **Materialized Paths**: Enable efficient hierarchical queries
2. **Proper Indexing**: Indexes on frequently queried columns
3. **Lazy Loading**: Folder tree uses lazy loading for large structures
4. **Caching**: Frequently accessed folders are cached

## Security

1. **Role-Based Access**: Permissions tied to user roles
2. **Application Scoping**: Folders isolated by application
3. **Corporate Boundaries**: Strict corporate-level isolation
4. **Audit Trail**: All changes tracked through BaseEntity

## Next Steps

After successful migration:

1. Test folder operations through the API
2. Verify permission enforcement
3. Test hierarchical queries performance
4. Monitor for any data integrity issues
5. Implement frontend components for folder management

## Support

For issues or questions regarding the migration:

1. Check application logs for detailed error messages
2. Use the validation endpoint to identify specific issues
3. Review the migration service code for troubleshooting
4. Consult the database views for data verification