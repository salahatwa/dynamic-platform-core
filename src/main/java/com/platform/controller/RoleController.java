package com.platform.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.platform.entity.Role;
import com.platform.enums.PermissionAction;
import com.platform.enums.PermissionResource;
import com.platform.repository.RoleRepository;
import com.platform.security.RequirePermission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Slf4j
public class RoleController {

	private final RoleRepository roleRepository;

	@GetMapping
	@RequirePermission(resource = PermissionResource.ROLES, action = PermissionAction.READ)
	public ResponseEntity<List<Role>> getAllRoles() {
		log.info("=== Fetching all roles ===");
		List<Role> roles = roleRepository.findAll();
		log.info("Found {} roles", roles.size());
		return ResponseEntity.ok(roles);
	}

	@GetMapping("/{id}")
	@RequirePermission(resource = PermissionResource.ROLES, action = PermissionAction.READ)
	public ResponseEntity<Role> getRoleById(@PathVariable Long id) {
		log.info("Fetching role with ID: {}", id);
		return roleRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}
}
