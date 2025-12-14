package com.platform.service;

import com.platform.entity.User;
import com.platform.repository.UserRepository;
import com.platform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Use the method that properly loads the corporate relationship
        User user = userRepository.findByEmailWithCorporate(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        // Debug logging for corporate isolation
        System.out.println("Loading user by email: " + email);
        System.out.println("User ID: " + user.getId());
        System.out.println("User corporate: " + (user.getCorporate() != null ? user.getCorporate().getId() : "NULL"));
        
        return UserPrincipal.create(user);
    }
    
    @Transactional
    public UserDetails loadUserById(Long id) {
        // Use the method that properly loads the corporate relationship
        User user = userRepository.findByIdWithCorporate(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        
        // Debug logging for corporate isolation
        System.out.println("Loading user by ID: " + id);
        System.out.println("User email: " + user.getEmail());
        System.out.println("User corporate: " + (user.getCorporate() != null ? user.getCorporate().getId() : "NULL"));
        
        return UserPrincipal.create(user);
    }
}
