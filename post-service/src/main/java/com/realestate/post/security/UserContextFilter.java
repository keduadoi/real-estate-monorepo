package com.realestate.post.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
@Slf4j
public class UserContextFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_EMAIL = "X-User-Email";
    private static final String HEADER_USER_ROLES = "X-User-Roles";
    private static final String HEADER_USER_NAME = "X-User-Name";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String userId = request.getHeader(HEADER_USER_ID);
            String email = request.getHeader(HEADER_USER_EMAIL);
            String roles = request.getHeader(HEADER_USER_ROLES);
            String name = request.getHeader(HEADER_USER_NAME);

            if (userId != null && !userId.isEmpty()) {
                UserInfo userInfo = UserInfo.builder()
                        .id(userId)
                        .email(email)
                        .name(name)
                        .rolesString(roles)
                        .build();
                UserContext.setCurrentUser(userInfo);

                log.debug("User context set: userId={}, email={}", userId, email);
            }

            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
