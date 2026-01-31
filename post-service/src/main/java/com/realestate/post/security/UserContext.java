package com.realestate.post.security;

import java.util.Optional;

public class UserContext {

    private static final ThreadLocal<UserInfo> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
        // Utility class
    }

    public static void setCurrentUser(UserInfo user) {
        CURRENT_USER.set(user);
    }

    public static Optional<UserInfo> getCurrentUser() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static Optional<String> getCurrentUserId() {
        return getCurrentUser().map(UserInfo::getId);
    }

    public static Optional<String> getCurrentUserEmail() {
        return getCurrentUser().map(UserInfo::getEmail);
    }

    public static boolean isAuthenticated() {
        return getCurrentUser().map(UserInfo::isAuthenticated).orElse(false);
    }

    public static boolean isAdmin() {
        return getCurrentUser().map(UserInfo::isAdmin).orElse(false);
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
