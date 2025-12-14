package com.platform.entity;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "corporates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Corporate extends BaseEntity {

	@Column(nullable = false, unique = true)
	private String name;

	@Column(nullable = false, unique = true)
	private String domain;

	private String description;

	@OneToMany(mappedBy = "corporate", cascade = CascadeType.ALL)
	@Builder.Default
	@JsonIgnore
	private Set<User> users = new HashSet<>();

	@OneToMany(mappedBy = "corporate", cascade = CascadeType.ALL)
	@Builder.Default
	@JsonIgnore
	private Set<ApiKey> apiKeys = new HashSet<>();

}
