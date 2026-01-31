package com.realestate.auth.service;

import com.realestate.auth.config.JwtConfig;
import com.realestate.auth.entity.Role;
import com.realestate.auth.entity.RsaKey;
import com.realestate.auth.entity.User;
import com.realestate.auth.repository.RefreshTokenRepository;
import com.realestate.auth.repository.RsaKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock private RsaKeyRepository rsaKeyRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    private JwtService jwtService;
    private RsaKey testRsaKey;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        JwtConfig config = new JwtConfig();
        JwtConfig.TokenConfig accessConfig = new JwtConfig.TokenConfig();
        accessConfig.setExpiration(900000);
        JwtConfig.TokenConfig refreshConfig = new JwtConfig.TokenConfig();
        refreshConfig.setExpiration(604800000);
        config.setAccessToken(accessConfig);
        config.setRefreshToken(refreshConfig);
        config.setIssuer("test-issuer");
        config.setAudience("test-audience");

        jwtService = new JwtService(rsaKeyRepository, refreshTokenRepository, config);

        // Generate test RSA key pair
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        testRsaKey = new RsaKey();
        testRsaKey.setId("test-kid");
        testRsaKey.setPublicKey(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
        testRsaKey.setPrivateKey(Base64.getEncoder().encodeToString(privateKey.getEncoded()));
        testRsaKey.setActive(true);
        testRsaKey.setExpiresAt(LocalDateTime.now().plusDays(90));

        Role role = new Role("ROLE_USER", "User");
        role.setId(1);

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setRoles(Set.of(role));
    }

    @Test
    void generateAccessToken_returnsValidToken() {
        when(rsaKeyRepository.findFirstByActiveTrue()).thenReturn(Optional.of(testRsaKey));

        String token = jwtService.generateAccessToken(testUser);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void validateToken_validToken_returnsClaims() {
        when(rsaKeyRepository.findFirstByActiveTrue()).thenReturn(Optional.of(testRsaKey));
        when(rsaKeyRepository.findById("test-kid")).thenReturn(Optional.of(testRsaKey));

        String token = jwtService.generateAccessToken(testUser);
        Map<String, Object> claims = jwtService.validateToken(token);

        assertNotNull(claims);
        assertEquals(testUser.getId().toString(), claims.get("sub"));
        assertEquals("test@example.com", claims.get("email"));
    }

    @Test
    void validateToken_invalidToken_returnsNull() {
        Map<String, Object> claims = jwtService.validateToken("invalid-token");
        assertNull(claims);
    }

    @Test
    void getJwks_returnsKeys() {
        when(rsaKeyRepository.findByActiveTrueOrExpiresAtAfter(any()))
                .thenReturn(List.of(testRsaKey));

        var response = jwtService.getJwks();

        assertNotNull(response);
        assertFalse(response.getKeys().isEmpty());
        assertEquals("test-kid", response.getKeys().get(0).get("kid"));
    }
}
