package com.platform.entity;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "template_folders", uniqueConstraints = @UniqueConstraint(name = "uk_name_parent_app", columnNames = {
		"name", "parent_id", "application_id" }), indexes = {
				@Index(name = "idx_template_folders_parent_app", columnList = "parent_id, application_id"),
				@Index(name = "idx_template_folders_app_corporate", columnList = "application_id, corporate_id"),
				@Index(name = "idx_template_folders_path", columnList = "path"),
				@Index(name = "idx_template_folders_corporate_id", columnList = "corporate_id") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TemplateFolder extends BaseEntity {

	@Column(nullable = false)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	@JsonIgnore
	private TemplateFolder parent;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "application_id", nullable = false)
	@JsonIgnore
	private App application;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "corporate_id", nullable = false)
	@JsonIgnore
	private Corporate corporate;

	@Column(nullable = false, length = 1000)
	private String path; // Materialized path for efficient queries

	@Column(nullable = false)
	@Builder.Default
	private Integer level = 0;

	@Column(name = "sort_order", nullable = false)
	@Builder.Default
	private Integer sortOrder = 0;

	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnore
	@Builder.Default
	private List<TemplateFolder> children = new ArrayList<>();

	@OneToMany(mappedBy = "folder", cascade = CascadeType.ALL)
	@JsonIgnore
	@Builder.Default
	private List<Template> templates = new ArrayList<>();

}
