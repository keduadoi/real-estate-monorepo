package com.realestate.backend.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter that extracts user information from Kong Gateway headers.
 *
 * Kong forwards JWT claims as headers after validating the token:
 * - X-User-Id: User UUID from JWT 'sub' claim
 * - X-User-Email: User email from JWT 'email' claim
 * - X-User-Roles: User roles from JWT 'roles' claim (comma-separated)
 *
 * This filter runs early in the chain to ensure UserContext is available
 * for all downstream processing.
 */
@Component
@Order(1)
@Slf4j
public class UserContextFilter implements Filter {

    // Kong header names (set by pre-function plugin)
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_USER_ROLES = "X-User-Roles";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // Extract user info from Kong headers
            String userId = httpRequest.getHeader(HEADER_USER_ID);
            String userEmail = httpRequest.getHeader(HEADER_USER_EMAIL);
            String userRoles = httpRequest.getHeader(HEADER_USER_ROLES);

            if (userId != null && !userId.isBlank()) {
                UserInfo userInfo = UserInfo.builder()
                        .id(userId)
                        .email(userEmail)
                        .rolesString(userRoles)
                        .build();

                UserContext.setCurrentUser(userInfo);

                log.debug("User context set: id={}, email={}, roles={}",
                        userId, userEmail, userRoles);
            } else {
                log.debug("No user context - public request to {}", httpRequest.getRequestURI());
            }

            // Continue with the filter chain
            chain.doFilter(request, response);

        } finally {
            // CRITICAL: Always clear context to prevent memory leaks
            UserContext.clear();
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("UserContextFilter initialized - listening for Kong headers: {}, {}, {}",
                HEADER_USER_ID, HEADER_USER_EMAIL, HEADER_USER_ROLES);
    }

    @Override
    public void destroy() {
        log.info("UserContextFilter destroyed");
    }
}
