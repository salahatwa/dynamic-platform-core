package com.platform.security;

import com.platform.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@Getter
public class UserPrincipal implements UserDetails {
    
    private Long id;
    private String email;
    private String password;
    private Long corporateId;
    private Collection<? extends GrantedAuthority> authorities;
    
    public static UserPrincipal create(User user) {
        Collection<GrantedAuthority> authorities = Stream.of(
            // Add roles with ROLE_ prefix (required by Spring Security)
            user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName())),
            // Add permissions from roles
            user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.getName())),
            // Add direct user permissions
            user.getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
        ).flatMap(s -> s).collect(Collectors.toSet());
        
        Long corporateId = user.getCorporate() != null ? user.getCorporate().getId() : null;
        
        return new UserPrincipal(
            user.getId(),
            user.getEmail(),
            user.getPassword(),
            corporateId,
            authorities
        );
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
}
