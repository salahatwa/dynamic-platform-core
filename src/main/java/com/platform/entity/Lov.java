package com.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "lov", indexes = { @Index(name = "idx_lov_code", columnList = "lov_code"),
		@Index(name = "idx_lov_type", columnList = "lov_type"),
		@Index(name = "idx_corporate_id", columnList = "corporate_id") })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lov {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "lov_code", nullable = false, length = 100)
	private String lovCode;

	@Column(name = "lov_type", nullable = false, length = 50)
	private String lovType;

	@Column(name = "lov_value", length = 500)
	private String lovValue;

	@Column(name = "attribute1", length = 200)
	private String attribute1;

	@Column(name = "attribute2", length = 200)
	private String attribute2;

	@Column(name = "attribute3", length = 200)
	private String attribute3;

	@Column(name = "display_order", nullable = false)
	private Integer displayOrder = 0;

	@Column(name = "active", nullable = false)
	private Boolean active = true;

	@Column(name = "parent_lov_id")
	private Long parentLovId;

	@Column(name = "translation_app", length = 100)
	private String translationApp;

	@Column(name = "translation_key", length = 200)
	private String translationKey;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "app_id")
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "corporate", "createdBy"})
	private App app;

	@Column(name = "metadata", columnDefinition = "TEXT")
	private String metadata; // JSON string

	@Column(name = "corporate_id")
	private Long corporateId;

	@Column(name = "version", nullable = false)
	private Integer version = 1;

	@Column(name = "created_by", nullable = false, length = 100)
	private String createdBy;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_by", nullable = false, length = 100)
	private String updatedBy;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}
