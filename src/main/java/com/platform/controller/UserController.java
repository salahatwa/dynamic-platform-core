package com.platform.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.platform.dto.UserDTO;
import com.platform.entity.User;
import com.platform.enums.PermissionAction;
import com.platform.enums.PermissionResource;
import com.platform.repository.UserRepository;
import com.platform.security.RequirePermission;
import com.platform.security.UserPrincipal;
import com.platform.service.AuditLogService;
import com.platform.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

	private final UserRepository userRepository;
	private final AuditLogService auditLogService;
	private final UserService userService;

	@GetMapping
	@RequirePermission(resource = PermissionResource.USERS, action = PermissionAction.READ)
	@Operation(summary = "Get all users with pagination (excluding current user)")
	public ResponseEntity<?> getAllUsers(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String search,
			@RequestParam(defaultValue = "false") boolean invitedByMe, Authentication authentication) {

		Long corporateId = getCorporateId();
		// Get current user ID to exclude from results
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Long currentUserId = ((UserPrincipal) auth.getPrincipal()).getId();
		
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

		Page<User> users;

		if (invitedByMe) {
			if (search != null && !search.isEmpty()) {
				users = userRepository.findByCorporateIdAndInvitedByIdAndIdNotWithSearch(
						corporateId, currentUserId, currentUserId, search, pageable);
			} else {
				users = userRepository.findByCorporateIdAndInvitedByIdAndIdNot(corporateId, currentUserId, currentUserId, pageable);
			}
		} else {
			if (search != null && !search.isEmpty()) {
				users = userRepository.findByCorporateIdAndIdNotWithSearch(
						corporateId, currentUserId, search, pageable);
			} else {
				users = userRepository.findByCorporateIdAndIdNot(corporateId, currentUserId, pageable);
			}
		}

		// Convert to DTOs to avoid Hibernate proxy serialization issues
		Page<UserDTO> userDTOs = users.map(UserDTO::fromEntity);
		return ResponseEntity.ok(userDTOs);
	}

	@GetMapping("/{id}")
	@RequirePermission(resource = PermissionResource.USERS, action = PermissionAction.READ)
	@Operation(summary = "Get user by ID")
	public ResponseEntity<?> getUserById(@PathVariable Long id) {
		Long corporateId = getCorporateId();
		User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

		// Verify user belongs to same corporate
		if (user.getCorporate() == null || !user.getCorporate().getId().equals(corporateId)) {
			return ResponseEntity.status(403).body("Access denied: User belongs to another organization");
		}

		return ResponseEntity.ok(UserDTO.fromEntity(user));
	}

	@PutMapping("/{id}/activate")
	@RequirePermission(resource = PermissionResource.USERS, action = PermissionAction.UPDATE)
	@Operation(summary = "Activate user")
	public ResponseEntity<?> activateUser(@PathVariable Long id, HttpServletRequest request) {
		Long corporateId = getCorporateId();
		User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

		// Verify user belongs to same corporate
		if (user.getCorporate() == null || !user.getCorporate().getId().equals(corporateId)) {
			return ResponseEntity.status(403).body("Access denied: User belongs to another organization");
		}

		user.setEnabled(true);
		userRepository.save(user);

		// Get current user for audit logging
		User currentUser = getCurrentUserWithCorporate();
		
		// Log audit
		auditLogService.log("ACTIVATE", "USER", id, user.getEmail(), currentUser, "Activated user account",
				request.getRemoteAddr());

		return ResponseEntity.ok().build();
	}

	@PutMapping("/{id}/deactivate")
	@RequirePermission(resource = PermissionResource.USERS, action = PermissionAction.UPDATE)
	@Operation(summary = "Deactivate user")
	public ResponseEntity<?> deactivateUser(@PathVariable Long id, HttpServletRequest request) {
		Long corporateId = getCorporateId();
		User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

		// Verify user belongs to same corporate
		if (user.getCorporate() == null || !user.getCorporate().getId().equals(corporateId)) {
			return ResponseEntity.status(403).body("Access denied: User belongs to another organization");
		}

		user.setEnabled(false);
		userRepository.save(user);

		// Get current user for audit logging
		User currentUser = getCurrentUserWithCorporate();
		
		// Log audit
		auditLogService.log("DEACTIVATE", "USER", id, user.getEmail(), currentUser, "Deactivated user account",
				request.getRemoteAddr());

		return ResponseEntity.ok().build();
	}

	@GetMapping("/{id}/deletion-impact")
	@RequirePermission(resource = PermissionResource.USERS, action = PermissionAction.DELETE)
	@Operation(summary = "Get user deletion impact information")
	public ResponseEntity<?> getUserDeletionImpact(@PathVariable Long id) {
		Long corporateId = getCorporateId();
		User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

		// Verify user belongs to same corporate
		if (user.getCorporate() == null || !user.getCorporate().getId().equals(corporateId)) {
			return ResponseEntity.status(403).body("Access denied: User belongs to another organization");
		}

		UserService.UserDeletionImpact impact = userService.getDeletionImpact(id);
		return ResponseEntity.ok(impact);
	}

	@DeleteMapping("/{id}")
	@RequirePermission(resource = PermissionResource.USERS, action = PermissionAction.DELETE)
	@Operation(summary = "Delete user")
	public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpServletRequest request) {
		Long corporateId = getCorporateId();
		User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

		// Verify user belongs to same corporate
		if (user.getCorporate() == null || !user.getCorporate().getId().equals(corporateId)) {
			return ResponseEntity.status(403).body("Access denied: User belongs to another organization");
		}

		// Get current user for self-deletion check and audit logging
		User currentUser = getCurrentUserWithCorporate();
		
		// Prevent self-deletion
		if (user.getId().equals(currentUser.getId())) {
			return ResponseEntity.badRequest().body("Cannot delete your own account");
		}

		String userEmail = user.getEmail();

		try {
			// Use the service to handle foreign key constraints properly
			userService.deleteUser(id);

			// Log audit
			auditLogService.log("DELETE", "USER", id, userEmail, currentUser,
					"Deleted user account with cascade handling", request.getRemoteAddr());

			return ResponseEntity.ok().build();

		} catch (Exception e) {
			// Log the error and return a user-friendly message
			auditLogService.log("DELETE_FAILED", "USER", id, userEmail, currentUser,
					"Failed to delete user: " + e.getMessage(), request.getRemoteAddr());

			return ResponseEntity.status(500).body("Failed to delete user: " + e.getMessage());
		}
	}

	@PutMapping("/{id}/soft-delete")
	@RequirePermission(resource = PermissionResource.USERS, action = PermissionAction.DELETE)
	@Operation(summary = "Soft delete user (deactivate)")
	public ResponseEntity<?> softDeleteUser(@PathVariable Long id, HttpServletRequest request) {
		Long corporateId = getCorporateId();
		User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

		// Verify user belongs to same corporate
		if (user.getCorporate() == null || !user.getCorporate().getId().equals(corporateId)) {
			return ResponseEntity.status(403).body("Access denied: User belongs to another organization");
		}

		// Get current user for self-deletion check and audit logging
		User currentUser = getCurrentUserWithCorporate();
		
		// Prevent self-deletion
		if (user.getId().equals(currentUser.getId())) {
			return ResponseEntity.badRequest().body("Cannot delete your own account");
		}

		String userEmail = user.getEmail();
		userService.softDeleteUser(id);

		// Log audit
		auditLogService.log("SOFT_DELETE", "USER", id, userEmail, currentUser,
				"Soft deleted user account (deactivated)", request.getRemoteAddr());

		return ResponseEntity.ok().build();
	}

	// Get corporate ID from authenticated user
	private Long getCorporateId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
			UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
			Long corporateId = userPrincipal.getCorporateId();
			
			if (corporateId == null) {
				throw new RuntimeException("User is not associated with any organization");
			}
			
			return corporateId;
		}
		
		throw new RuntimeException("Authentication required");
	}

	private User getCurrentUserWithCorporate() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
			UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
			return userRepository.findByIdWithCorporate(userPrincipal.getId()).orElse(null);
		}
		return null;
	}
}
