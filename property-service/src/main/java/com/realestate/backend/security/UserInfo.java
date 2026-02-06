package com.realestate.backend.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User information extracted from Kong JWT headers.
 * This class holds the authenticated user's details forwarded by Kong Gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    /**
     * User ID (UUID) from JWT 'sub' claim
     */
    private String id;

    /**
     * User email from JWT 'email' claim
     */
    private String email;

    /**
     * User roles from JWT 'roles' claim (e.g., "ROLE_USER,ROLE_ADMIN")
     */
    private String rolesString;

    /**
     * Parsed list of roles
     */
    public List<String> getRoles() {
        if (rolesString == null || rolesString.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(rolesString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String role) {
        return getRoles().contains(role);
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    /**
     * Check if user is authenticated (has valid ID)
     */
    public boolean isAuthenticated() {
        return id != null && !id.isBlank();
    }

    @Override
    public String toString() {
        return "UserInfo{id='" + id + "', email='" + email + "', roles=" + getRoles() + "}";
    }
}
