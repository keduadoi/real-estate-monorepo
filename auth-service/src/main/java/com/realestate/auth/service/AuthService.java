package com.realestate.auth.service;

import com.realestate.auth.config.JwtConfig;
import com.realestate.auth.config.MetricsConfig.AuthMetrics;
import com.realestate.auth.dto.request.ChangePasswordRequest;
import com.realestate.auth.dto.request.LoginRequest;
import com.realestate.auth.dto.request.RefreshRequest;
import com.realestate.auth.dto.request.RegisterRequest;
import com.realestate.auth.dto.response.AuthResponse;
import com.realestate.auth.dto.response.TokenResponse;
import com.realestate.auth.dto.response.UserResponse;
import com.realestate.auth.entity.RefreshToken;
import com.realestate.auth.entity.Role;
import com.realestate.auth.entity.User;
import com.realestate.auth.exception.*;
import com.realestate.auth.repository.RefreshTokenRepository;
import com.realestate.auth.repository.RoleRepository;
import com.realestate.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final AuthMetrics authMetrics;

    @Value("${security.account.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${security.account.lock-duration-minutes:30}")
    private int lockDurationMinutes;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtConfig jwtConfig,
                       AuthMetrics authMetrics) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtConfig = jwtConfig;
        this.authMetrics = authMetrics;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        authMetrics.recordRegistrationAttempt();
        return authMetrics.getRegistrationLatency().record(() -> {
            try {
                if (userRepository.existsByEmail(request.getEmail())) {
                    authMetrics.recordRegistrationFailure();
                    throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
                }

                User user = new User();
                user.setEmail(request.getEmail());
                user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                user.setFirstName(request.getFirstName());
                user.setLastName(request.getLastName());
                user.setPhone(request.getPhone());

                Role userRole = roleRepository.findByName("ROLE_USER")
                        .orElseThrow(() -> new RuntimeException("Default role not found"));
                Set<Role> roles = new HashSet<>();
                roles.add(userRole);
                user.setRoles(roles);

                user = userRepository.save(user);
                authMetrics.recordRegistrationSuccess();

                return buildAuthResponse(user, null, null);
            } catch (EmailAlreadyExistsException e) {
                throw e;
            } catch (Exception e) {
                authMetrics.recordRegistrationFailure();
                throw e;
            }
        });
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String deviceInfo, String ipAddress) {
        authMetrics.recordLoginAttempt();
        return authMetrics.getLoginLatency().record(() -> {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> {
                        authMetrics.recordLoginFailure();
                        return new InvalidCredentialsException("Invalid email or password");
                    });

            if (user.isAccountLocked()) {
                authMetrics.recordLoginFailure();
                throw new AccountLockedException("Account is locked. Please try again later.");
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
                    user.setAccountLocked(true);
                    authMetrics.recordAccountLockout();
                }
                userRepository.save(user);
                authMetrics.recordLoginFailure();
                throw new InvalidCredentialsException("Invalid email or password");
            }

            user.setFailedLoginAttempts(0);
            user.setAccountLocked(false);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            authMetrics.recordLoginSuccess();
            return buildAuthResponse(user, deviceInfo, ipAddress);
        });
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        authMetrics.recordTokenRefreshAttempt();
        try {
            RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                    .orElseThrow(() -> {
                        authMetrics.recordTokenRefreshFailure();
                        return new InvalidTokenException("Invalid refresh token");
                    });

            if (refreshToken.isRevoked()) {
                authMetrics.recordTokenRefreshFailure();
                throw new InvalidTokenException("Refresh token has been revoked");
            }

            if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                authMetrics.recordTokenRefreshFailure();
                throw new TokenExpiredException("Refresh token has expired");
            }

            String accessToken = jwtService.generateAccessToken(refreshToken.getUser());
            authMetrics.recordTokenRefreshSuccess();
            return new TokenResponse(accessToken, jwtConfig.getAccessToken().getExpiration() / 1000);
        } catch (InvalidTokenException | TokenExpiredException e) {
            throw e;
        } catch (Exception e) {
            authMetrics.recordTokenRefreshFailure();
            throw e;
        }
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUser(user);
    }

    private AuthResponse buildAuthResponse(User user, String deviceInfo, String ipAddress) {
        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = jwtService.generateRefreshToken(user, deviceInfo, ipAddress);
        UserResponse userResponse = UserResponse.fromEntity(user);
        return new AuthResponse(accessToken, refreshToken.getToken(),
                jwtConfig.getAccessToken().getExpiration() / 1000, userResponse);
    }
}
