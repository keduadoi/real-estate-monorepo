package com.realestate.auth.service;

import com.realestate.auth.config.JwtConfig;
import com.realestate.auth.dto.request.LoginRequest;
import com.realestate.auth.dto.request.RegisterRequest;
import com.realestate.auth.dto.response.AuthResponse;
import com.realestate.auth.entity.RefreshToken;
import com.realestate.auth.entity.Role;
import com.realestate.auth.entity.User;
import com.realestate.auth.exception.EmailAlreadyExistsException;
import com.realestate.auth.exception.InvalidCredentialsException;
import com.realestate.auth.repository.RefreshTokenRepository;
import com.realestate.auth.repository.RoleRepository;
import com.realestate.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private JwtConfig jwtConfig;

    @InjectMocks
    private AuthService authService;

    private Role userRole;
    private User testUser;
    private JwtConfig.TokenConfig accessTokenConfig;

    @BeforeEach
    void setUp() {
        userRole = new Role("ROLE_USER", "Standard user");
        userRole.setId(1);

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRoles(Set.of(userRole));
        testUser.setCreatedAt(LocalDateTime.now());

        accessTokenConfig = new JwtConfig.TokenConfig();
        accessTokenConfig.setExpiration(900000);
    }

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setFirstName("New");
        request.setLastName("User");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(LocalDateTime.now());
            return u;
        });
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        RefreshToken rt = new RefreshToken();
        rt.setToken("refresh-token");
        when(jwtService.generateRefreshToken(any(), any(), any())).thenReturn(rt);
        when(jwtConfig.getAccessToken()).thenReturn(accessTokenConfig);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_emailExists_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        RefreshToken rt = new RefreshToken();
        rt.setToken("refresh-token");
        when(jwtService.generateRefreshToken(any(), any(), any())).thenReturn(rt);
        when(jwtConfig.getAccessToken()).thenReturn(accessTokenConfig);

        AuthResponse response = authService.login(request, "Chrome", "127.0.0.1");

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
    }

    @Test
    void login_invalidPassword_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(request, null, null));
    }

    @Test
    void login_userNotFound_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(request, null, null));
    }
}
