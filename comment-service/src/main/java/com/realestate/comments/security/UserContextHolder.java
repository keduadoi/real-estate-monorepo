package com.realestate.comments.security;

/**
 * Per-request holder for the authenticated user. Populated by
 * {@link UserContextFilter} and read in services / controllers.
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {}

    public static UserContext get() {
        UserContext ctx = CONTEXT.get();
        return ctx == null ? UserContext.ANONYMOUS : ctx;
    }

    public static void set(UserContext ctx) {
        CONTEXT.set(ctx);
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
