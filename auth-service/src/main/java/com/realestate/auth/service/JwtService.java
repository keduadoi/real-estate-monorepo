package com.realestate.auth.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.realestate.auth.config.JwtConfig;
import com.realestate.auth.dto.response.JwksResponse;
import com.realestate.auth.entity.RefreshToken;
import com.realestate.auth.entity.User;
import com.realestate.auth.repository.RefreshTokenRepository;
import com.realestate.auth.repository.RsaKeyRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final RsaKeyRepository rsaKeyRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtConfig jwtConfig;

    public JwtService(RsaKeyRepository rsaKeyRepository,
                      RefreshTokenRepository refreshTokenRepository,
                      JwtConfig jwtConfig) {
        this.rsaKeyRepository = rsaKeyRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtConfig = jwtConfig;
    }

    @PostConstruct
    @Transactional
    public void initializeKeys() {
        if (rsaKeyRepository.findFirstByActiveTrue().isEmpty()) {
            generateAndStoreKeyPair();
        }
    }

    private com.realestate.auth.entity.RsaKey generateAndStoreKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

            String kid = UUID.randomUUID().toString();
            RSAKey jwk = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(kid)
                    .build();

            com.realestate.auth.entity.RsaKey rsaKey = new com.realestate.auth.entity.RsaKey();
            rsaKey.setId(kid);
            rsaKey.setPublicKey(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
            rsaKey.setPrivateKey(Base64.getEncoder().encodeToString(privateKey.getEncoded()));
            rsaKey.setActive(true);
            rsaKey.setExpiresAt(LocalDateTime.now().plusDays(90));

            return rsaKeyRepository.save(rsaKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    public String generateAccessToken(User user) {
        try {
            com.realestate.auth.entity.RsaKey rsaKey = rsaKeyRepository.findFirstByActiveTrue()
                    .orElseThrow(() -> new RuntimeException("No active RSA key found"));

            byte[] privateKeyBytes = Base64.getDecoder().decode(rsaKey.getPrivateKey());
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) kf.generatePrivate(
                    new java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes));

            List<String> roles = user.getRoles().stream()
                    .map(r -> r.getName())
                    .collect(Collectors.toList());

            Date now = new Date();
            Date expiry = new Date(now.getTime() + jwtConfig.getAccessToken().getExpiration());

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(user.getId().toString())
                    .claim("email", user.getEmail())
                    .claim("roles", roles)
                    .issuer(jwtConfig.getIssuer())
                    .audience(jwtConfig.getAudience())
                    .issueTime(now)
                    .expirationTime(expiry)
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(rsaKey.getId())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(new RSASSASigner(privateKey));

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate access token", e);
        }
    }

    @Transactional
    public RefreshToken generateRefreshToken(User user, String deviceInfo, String ipAddress) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(jwtConfig.getRefreshToken().getExpiration() / 1000));
        refreshToken.setDeviceInfo(deviceInfo);
        refreshToken.setIpAddress(ipAddress);
        return refreshTokenRepository.save(refreshToken);
    }

    public Map<String, Object> validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String kid = signedJWT.getHeader().getKeyID();

            com.realestate.auth.entity.RsaKey rsaKey = rsaKeyRepository.findById(kid)
                    .orElseThrow(() -> new RuntimeException("Key not found: " + kid));

            byte[] publicKeyBytes = Base64.getDecoder().decode(rsaKey.getPublicKey());
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(
                    new java.security.spec.X509EncodedKeySpec(publicKeyBytes));

            if (!signedJWT.verify(new RSASSAVerifier(publicKey))) {
                return null;
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            if (claims.getExpirationTime().before(new Date())) {
                return null;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("sub", claims.getSubject());
            result.put("email", claims.getStringClaim("email"));
            result.put("roles", claims.getStringListClaim("roles"));
            return result;
        } catch (ParseException | java.security.NoSuchAlgorithmException |
                 java.security.spec.InvalidKeySpecException | JOSEException e) {
            log.error("Token validation failed", e);
            return null;
        }
    }

    public JwksResponse getJwks() {
        List<com.realestate.auth.entity.RsaKey> keys =
                rsaKeyRepository.findByActiveTrueOrExpiresAtAfter(LocalDateTime.now());

        List<Map<String, Object>> jwkList = keys.stream().map(rsaKey -> {
            try {
                byte[] publicKeyBytes = Base64.getDecoder().decode(rsaKey.getPublicKey());
                java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
                RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(
                        new java.security.spec.X509EncodedKeySpec(publicKeyBytes));

                RSAKey jwk = new RSAKey.Builder(publicKey)
                        .keyID(rsaKey.getId())
                        .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                        .algorithm(JWSAlgorithm.RS256)
                        .build();

                return jwk.toJSONObject();
            } catch (Exception e) {
                log.error("Failed to build JWK for key: {}", rsaKey.getId(), e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        return new JwksResponse(jwkList);
    }

    /**
     * Rotate RSA keys for JWT signing.
     * This method:
     * 1. Creates a new key pair
     * 2. Sets the new key as active
     * 3. Keeps old keys active for a grace period to allow existing tokens to validate
     * 4. Marks very old keys as inactive
     *
     * @param gracePeriodDays Number of days to keep old keys active for validation
     * @return The ID of the newly created key
     */
    @Transactional
    public String rotateKeys(int gracePeriodDays) {
        log.info("Starting key rotation with grace period of {} days", gracePeriodDays);

        // Step 1: Generate new key pair
        com.realestate.auth.entity.RsaKey newKey = generateAndStoreKeyPair();
        log.info("Generated new RSA key with ID: {}", newKey.getId());

        // Step 2: Update expiration of old active keys (grace period for token validation)
        List<com.realestate.auth.entity.RsaKey> activeKeys = rsaKeyRepository.findByActiveTrue();
        LocalDateTime gracePeriodEnd = LocalDateTime.now().plusDays(gracePeriodDays);

        for (com.realestate.auth.entity.RsaKey key : activeKeys) {
            if (!key.getId().equals(newKey.getId())) {
                // Keep the key for validation but update its expiration
                if (key.getExpiresAt().isAfter(gracePeriodEnd)) {
                    key.setExpiresAt(gracePeriodEnd);
                    rsaKeyRepository.save(key);
                    log.info("Updated key {} expiration to grace period end: {}", key.getId(), gracePeriodEnd);
                }
            }
        }

        // Step 3: Deactivate keys that have passed their expiration
        List<com.realestate.auth.entity.RsaKey> expiredKeys =
                rsaKeyRepository.findByActiveTrueAndExpiresAtBefore(LocalDateTime.now());
        for (com.realestate.auth.entity.RsaKey key : expiredKeys) {
            key.setActive(false);
            rsaKeyRepository.save(key);
            log.info("Deactivated expired key: {}", key.getId());
        }

        // Step 4: Delete keys that have been inactive for more than 30 days
        LocalDateTime deleteThreshold = LocalDateTime.now().minusDays(30);
        int deletedCount = rsaKeyRepository.deleteByActiveFalseAndExpiresAtBefore(deleteThreshold);
        if (deletedCount > 0) {
            log.info("Deleted {} old inactive keys", deletedCount);
        }

        log.info("Key rotation completed. New active key ID: {}", newKey.getId());
        return newKey.getId();
    }

    /**
     * Check if key rotation is needed based on the age of the current active key.
     *
     * @param maxAgeInDays Maximum age of the active key before rotation is needed
     * @return true if rotation is needed, false otherwise
     */
    public boolean isRotationNeeded(int maxAgeInDays) {
        Optional<com.realestate.auth.entity.RsaKey> activeKey = rsaKeyRepository.findFirstByActiveTrue();
        if (activeKey.isEmpty()) {
            return true;
        }

        LocalDateTime rotationThreshold = LocalDateTime.now().minusDays(maxAgeInDays);
        // Check if key was created before the threshold (key createdAt is the current time minus expiration)
        // Since we don't have createdAt, we'll use expiresAt - 90 days as approximation
        LocalDateTime approximateCreatedAt = activeKey.get().getExpiresAt().minusDays(90);
        return approximateCreatedAt.isBefore(rotationThreshold);
    }

    /**
     * Get statistics about current keys for monitoring.
     */
    public Map<String, Object> getKeyStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<com.realestate.auth.entity.RsaKey> activeKeys = rsaKeyRepository.findByActiveTrue();
        List<com.realestate.auth.entity.RsaKey> allKeys =
                rsaKeyRepository.findByActiveTrueOrExpiresAtAfter(LocalDateTime.now().minusDays(30));

        stats.put("activeKeyCount", activeKeys.size());
        stats.put("totalKeyCount", allKeys.size());

        if (!activeKeys.isEmpty()) {
            com.realestate.auth.entity.RsaKey primaryKey = activeKeys.get(0);
            stats.put("primaryKeyId", primaryKey.getId());
            stats.put("primaryKeyExpiresAt", primaryKey.getExpiresAt().toString());
        }

        return stats;
    }
}
