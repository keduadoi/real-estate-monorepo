package com.realestate.news.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.news.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Allows public reads on /api/news but rejects writes (POST/PUT/DELETE)
 * unless the request carries X-User-Roles containing ROLE_ADMIN.
 *
 * The X-User-* headers are populated by the frontend after a NextAuth login —
 * matching the existing platform pattern used by property-service and post-service.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class AdminWriteAuthorizationFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ROLES = "X-User-Roles";
    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final String NEWS_PATH_PREFIX = "/api/news";
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path != null && path.startsWith(NEWS_PATH_PREFIX) && WRITE_METHODS.contains(method)) {
            String rolesHeader = request.getHeader(HEADER_USER_ROLES);
            if (!hasAdminRole(rolesHeader)) {
                log.warn("Blocked {} {} — missing ROLE_ADMIN (roles header={})",
                        method, path, rolesHeader);
                writeForbidden(response, rolesHeader == null || rolesHeader.isBlank()
                        ? "Authentication required"
                        : "Admin role required");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean hasAdminRole(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return false;
        }
        for (String role : rolesHeader.split(",")) {
            if (ADMIN_ROLE.equalsIgnoreCase(role.trim())) {
                return true;
            }
        }
        return false;
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        ErrorResponse body = new ErrorResponse("FORBIDDEN", message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
