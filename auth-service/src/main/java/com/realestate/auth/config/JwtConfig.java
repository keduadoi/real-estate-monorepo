package com.realestate.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private TokenConfig accessToken = new TokenConfig();
    private TokenConfig refreshToken = new TokenConfig();
    private String issuer = "auth-service";
    private String audience = "real-estate-api";

    public static class TokenConfig {
        private long expiration;

        public long getExpiration() { return expiration; }
        public void setExpiration(long expiration) { this.expiration = expiration; }
    }

    public TokenConfig getAccessToken() { return accessToken; }
    public void setAccessToken(TokenConfig accessToken) { this.accessToken = accessToken; }
    public TokenConfig getRefreshToken() { return refreshToken; }
    public void setRefreshToken(TokenConfig refreshToken) { this.refreshToken = refreshToken; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
}
