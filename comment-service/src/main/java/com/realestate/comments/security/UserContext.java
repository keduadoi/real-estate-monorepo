package com.realestate.comments.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Snapshot of the authenticated user from Kong's X-User-* headers.
 * `userId == null` means anonymous.
 */
@Getter
@RequiredArgsConstructor
public class UserContext {

    public static final UserContext ANONYMOUS = new UserContext(null, null, null, List.of());

    private final String userId;
    private final String email;
    private final String name;
    private final List<String> roles;

    public boolean isAuthenticated() {
        return userId != null;
    }

    public boolean isAdmin() {
        return roles != null && roles.contains("ROLE_ADMIN");
    }
}
