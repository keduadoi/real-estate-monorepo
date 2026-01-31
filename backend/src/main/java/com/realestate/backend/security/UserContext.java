package com.realestate.backend.security;

import java.util.Optional;

/**
 * Thread-local holder for the current authenticated user.
 * User information is set by UserContextFilter from Kong headers.
 *
 * Usage:
 * <pre>
 * UserInfo user = UserContext.getCurrentUser()
 *     .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
 * String userId = user.getId();
 * </pre>
 */
public final class UserContext {

    private static final ThreadLocal<UserInfo> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
        // Utility class - prevent instantiation
    }

    /**
     * Set the current user for this thread.
     * Called by UserContextFilter when processing requests.
     *
     * @param user The authenticated user info
     */
    public static void setCurrentUser(UserInfo user) {
        CURRENT_USER.set(user);
    }

    /**
     * Get the current authenticated user.
     *
     * @return Optional containing the user if authenticated, empty otherwise
     */
    public static Optional<UserInfo> getCurrentUser() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    /**
     * Get the current user or throw an exception if not authenticated.
     *
     * @return The current user
     * @throws IllegalStateException if no user is set
     */
    public static UserInfo requireCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("No authenticated user in context"));
    }

    /**
     * Get the current user's ID.
     *
     * @return Optional containing the user ID if authenticated
     */
    public static Optional<String> getCurrentUserId() {
        return getCurrentUser().map(UserInfo::getId);
    }

    /**
     * Check if there is an authenticated user in the current context.
     *
     * @return true if a user is authenticated
     */
    public static boolean isAuthenticated() {
        return getCurrentUser()
                .map(UserInfo::isAuthenticated)
                .orElse(false);
    }

    /**
     * Check if the current user has admin role.
     *
     * @return true if the current user is an admin
     */
    public static boolean isAdmin() {
        return getCurrentUser()
                .map(UserInfo::isAdmin)
                .orElse(false);
    }

    /**
     * Clear the current user from thread-local storage.
     * MUST be called after request processing to prevent memory leaks.
     */
    public static void clear() {
        CURRENT_USER.remove();
    }
}
