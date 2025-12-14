package com.platform.security;

import com.platform.entity.ApiKey;
import com.platform.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        
        // Only apply to content API endpoints
        if (!requestPath.startsWith("/api/content/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Missing or invalid Authorization header. Use: Authorization: Bearer tms_your_key\"}");
            return;
        }

        String apiKeyValue = authHeader.substring(7); // Remove "Bearer " prefix
        
        if (!apiKeyValue.startsWith("tms_")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid API key format. API keys must start with 'tms_'\"}");
            return;
        }

        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyValueWithApp(apiKeyValue);
        
        if (apiKeyOpt.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid API key\"}");
            return;
        }

        ApiKey apiKey = apiKeyOpt.get();
        
        // Check if API key is active
        if (!apiKey.getActive()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"API key is inactive\"}");
            return;
        }

        // Check if API key is expired
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"API key has expired\"}");
            return;
        }

        // Update last used timestamp
        try {
            apiKey.setLastUsedAt(LocalDateTime.now());
            apiKeyRepository.save(apiKey);
        } catch (Exception e) {
            log.warn("Failed to update API key last used timestamp: {}", e.getMessage());
        }

        // Set app ID in request attributes for controllers to use
        request.setAttribute("appId", apiKey.getApp().getId());
        request.setAttribute("apiKey", apiKey);

        // Set authentication in SecurityContext to prevent 403 errors
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            "api-key-user", // principal
            null, // credentials
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_USER")) // authorities
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("API key authentication successful for app: {} ({})", 
                 apiKey.getApp().getName(), apiKey.getApp().getId());

        filterChain.doFilter(request, response);
    }
}