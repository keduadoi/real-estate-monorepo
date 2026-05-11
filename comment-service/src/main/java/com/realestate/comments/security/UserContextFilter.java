package com.realestate.comments.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reads X-User-* headers populated by Kong (after NextAuth login) and stashes
 * them on the per-request {@link UserContextHolder}. Absent headers => anonymous.
 *
 * Same shape as property-service's UserContextFilter — keeps the platform
 * pattern consistent.
 */
@Component
@Order(1)
public class UserContextFilter extends OncePerRequestFilter {

    private static final String H_USER_ID = "X-User-Id";
    private static final String H_USER_EMAIL = "X-User-Email";
    private static final String H_USER_NAME = "X-User-Name";
    private static final String H_USER_ROLES = "X-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String userId = trimToNull(request.getHeader(H_USER_ID));
            if (userId != null) {
                String email = trimToNull(request.getHeader(H_USER_EMAIL));
                String name = trimToNull(request.getHeader(H_USER_NAME));
                List<String> roles = parseRoles(request.getHeader(H_USER_ROLES));
                UserContextHolder.set(new UserContext(userId, email, name, roles));
            }
            chain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static List<String> parseRoles(String header) {
        if (header == null || header.isBlank()) return List.of();
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableList());
    }
}
