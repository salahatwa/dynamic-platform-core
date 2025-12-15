package com.platform.controller;

import com.platform.dto.LoginRequest;
import com.platform.dto.RegisterRequest;
import com.platform.dto.AuthResponse;
import com.platform.entity.User;
import com.platform.entity.Role;
import com.platform.enums.AuthProvider;
import com.platform.repository.UserRepository;
import com.platform.repository.RoleRepository;
import com.platform.repository.CorporateRepository;
import com.platform.security.JwtTokenProvider;
import com.platform.entity.Corporate;
import com.platform.entity.Invitation;
import com.platform.enums.InvitationStatus;
import com.platform.repository.InvitationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InvitationRepository invitationRepository;
    private final CorporateRepository corporateRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final com.platform.service.SubscriptionService subscriptionService;
    private final com.platform.service.PermissionService permissionService;

    @PostMapping("/register")
    @Operation(summary = "Register new user")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            String finalEmail = request.getEmail();
            if (request.getInvitationToken() != null && !request.getInvitationToken().isEmpty()) {
                Invitation invitation = invitationRepository.findByToken(request.getInvitationToken())
                        .orElseThrow(() -> new RuntimeException("Invalid invitation token"));
                if (invitation.getStatus() != InvitationStatus.PENDING) {
                    throw new RuntimeException("Invitation has already been used or is no longer valid");
                }
                if (invitation.getExpiresAt() != null
                        && invitation.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
                    throw new RuntimeException("Invitation has expired. Please request a new invitation");
                }
                finalEmail = invitation.getEmail();
            }
            if (userRepository.existsByEmail(finalEmail)) {
                return ResponseEntity.badRequest().body(new ErrorResponse(
                        "An account with this email already exists. Please login or use a different email"));
            }

            boolean isFirstUser = userRepository.count() == 0;

            User user = new User();
            user.setEmail(finalEmail);
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setName(request.getFirstName() + " " + request.getLastName());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setProvider(AuthProvider.LOCAL);
            user.setEnabled(true);

            // Handle invitation-based registration
            if (request.getInvitationToken() != null && !request.getInvitationToken().isEmpty()) {
                Invitation invitation = invitationRepository.findByToken(request.getInvitationToken())
                        .orElseThrow(() -> new RuntimeException("Invalid invitation token"));
                user.setInvitedBy(invitation.getInvitedBy());
                user.setCorporate(invitation.getCorporate());
                invitation.setStatus(InvitationStatus.ACCEPTED);
                invitation.setAcceptedAt(java.time.LocalDateTime.now());
                invitationRepository.save(invitation);
            } else {
                // Auto-create corporate for self-registration
                Corporate corporate = createCorporateForUser(user);
                user.setCorporate(corporate);

                // Create default subscription for the new corporate
                try {
                    subscriptionService.createDefaultSubscription(corporate.getId());
                } catch (Exception e) {
                    // Log error but don't fail registration? Or fail?
                    // Better to fail so we don't end up with inconsistent state
                    throw new RuntimeException("Failed to create subscription for new organization", e);
                }
            }

            userRepository.save(user);

            // Assign roles based on registration type
            if (isFirstUser) {
                // First user becomes SUPER_ADMIN
                Role superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                        .orElseThrow(() -> new RuntimeException("SUPER_ADMIN role not found. Please run application initialization."));
                user.getRoles().add(superAdminRole);
                userRepository.save(user);
            } else if (request.getInvitationToken() == null || request.getInvitationToken().isEmpty()) {
                // Users who self-register (no invitation) become ADMIN of their own corporate
                Role adminRole = roleRepository.findByName("ADMIN")
                        .orElseThrow(() -> new RuntimeException("ADMIN role not found. Please run application initialization."));
                user.getRoles().add(adminRole);
                userRepository.save(user);
            } else {
                // Users who register via invitation get roles specified in invitation
                Invitation invitation = invitationRepository.findByToken(request.getInvitationToken())
                        .orElseThrow(() -> new RuntimeException("Invalid invitation token"));
                
                if (!invitation.getRoles().isEmpty()) {
                    user.getRoles().addAll(invitation.getRoles());
                } else {
                    // Default to EDITOR role if no roles specified in invitation
                    Role editorRole = roleRepository.findByName("EDITOR")
                            .orElseThrow(() -> new RuntimeException("EDITOR role not found. Please run application initialization."));
                    user.getRoles().add(editorRole);
                }
                userRepository.save(user);
            }

            return ResponseEntity.ok(new MessageResponse("User registered successfully"));
        } catch (RuntimeException e) {
            // Handle known runtime exceptions with user-friendly messages
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            // Handle unexpected exceptions
            e.printStackTrace();
            String errorMessage = "Registration failed. Please try again";

            // Check for specific database constraint violations
            if (e.getMessage() != null) {
                if (e.getMessage().contains("duplicate key") && e.getMessage().contains("email")) {
                    errorMessage = "An account with this email already exists";
                } else if (e.getMessage().contains("duplicate key") && e.getMessage().contains("name")) {
                    errorMessage = "Organization name already exists. Please try again";
                } else if (e.getMessage().contains("duplicate key")) {
                    errorMessage = "This information is already registered. Please try different details";
                }
            }

            return ResponseEntity.badRequest().body(new ErrorResponse(errorMessage));
        }
    }

    record MessageResponse(String message) {
    }

    record ErrorResponse(String message) {
    }

    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        String token = tokenProvider.generateToken(authentication);
        
        // Get user details and permissions
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get user permissions using PermissionService
        Set<String> permissions = permissionService.getUserPermissions(user);
        
        // Build user info
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .corporateId(user.getCorporate() != null ? user.getCorporate().getId() : null)
                .corporateName(user.getCorporate() != null ? user.getCorporate().getName() : null)
                .build();
        
        AuthResponse response = AuthResponse.builder()
                .token(token)
                .permissions(permissions)
                .user(userInfo)
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a corporate for a user during self-registration
     */
    private Corporate createCorporateForUser(User user) {
        // Generate corporate name from user's name
        String baseCorporateName = user.getName() + "'s Organization";
        String corporateName = baseCorporateName;
        int nameCounter = 1;

        // Ensure corporate name uniqueness
        while (corporateRepository.existsByName(corporateName)) {
            corporateName = baseCorporateName + " " + nameCounter;
            nameCounter++;
        }

        // Generate unique domain from email or UUID
        String baseDomain = extractDomainFromEmail(user.getEmail());
        String domain = baseDomain;
        int domainCounter = 1;

        // Ensure domain uniqueness
        while (corporateRepository.existsByDomain(domain)) {
            domain = baseDomain + domainCounter;
            domainCounter++;
        }

        Corporate corporate = Corporate.builder()
                .name(corporateName)
                .domain(domain)
                .description("Auto-created organization for " + user.getName())
                .build();

        return corporateRepository.save(corporate);
    }

    /**
     * Extracts a domain-safe string from email
     */
    private String extractDomainFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "org-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        }

        // Get the part before @ and sanitize it
        String localPart = email.substring(0, email.indexOf("@"));
        // Remove special characters and convert to lowercase
        String sanitized = localPart.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        if (sanitized.isEmpty()) {
            return "org-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        }

        return sanitized;
    }
}
