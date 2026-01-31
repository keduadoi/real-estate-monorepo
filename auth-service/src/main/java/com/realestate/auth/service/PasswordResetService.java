package com.realestate.auth.service;

import com.realestate.auth.config.MetricsConfig.AuthMetrics;
import com.realestate.auth.dto.request.ResetPasswordRequest;
import com.realestate.auth.entity.PasswordResetToken;
import com.realestate.auth.entity.User;
import com.realestate.auth.exception.InvalidTokenException;
import com.realestate.auth.exception.TokenExpiredException;
import com.realestate.auth.repository.PasswordResetTokenRepository;
import com.realestate.auth.repository.RefreshTokenRepository;
import com.realestate.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMetrics authMetrics;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository passwordResetTokenRepository,
                                RefreshTokenRepository refreshTokenRepository,
                                PasswordEncoder passwordEncoder,
                                AuthMetrics authMetrics) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authMetrics = authMetrics;
    }

    @Transactional
    public void forgotPassword(String email) {
        authMetrics.recordPasswordResetRequest();
        userRepository.findByEmail(email).ifPresent(user -> {
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setToken(UUID.randomUUID().toString());
            token.setExpiresAt(LocalDateTime.now().plusHours(1));
            passwordResetTokenRepository.save(token);

            // TODO: Send email with reset link
            log.info("Password reset token generated for user {}: {}", email, token.getToken());
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));

        if (token.isUsed()) {
            throw new InvalidTokenException("Password reset token has already been used");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Password reset token has expired");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        refreshTokenRepository.revokeAllByUser(user);
    }
}
