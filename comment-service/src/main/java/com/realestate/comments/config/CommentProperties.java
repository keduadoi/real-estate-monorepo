package com.realestate.comments.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "comments")
public class CommentProperties {

    /** Global kill-switch — when true, all writes return 503. */
    private boolean disabled = false;

    /** HMAC key for hashing client IPs — rotate yearly to invalidate old hashes. */
    private String ipHashSecret = "dev-only-rotate-me-in-prod-please";

    private int editWindowMinutes = 15;
    private int bodyMinLength = 1;
    private int bodyMaxLength = 1000;
    private int displayNameMaxLength = 50;

    private RateLimit rateLimit = new RateLimit();
    private Captcha captcha = new Captcha();
    private PropertyService propertyService = new PropertyService();

    public static class RateLimit {
        private int anonPerIpPer5min = 5;
        private int anonPerIpPerDay = 30;
        private int userPerIdPer5min = 30;
        private int userPerIdPerDay = 200;
        private int perPropertyPerMin = 20;
        private int likePerIdentityPerMin = 60;

        public int getAnonPerIpPer5min() { return anonPerIpPer5min; }
        public void setAnonPerIpPer5min(int v) { this.anonPerIpPer5min = v; }
        public int getAnonPerIpPerDay() { return anonPerIpPerDay; }
        public void setAnonPerIpPerDay(int v) { this.anonPerIpPerDay = v; }
        public int getUserPerIdPer5min() { return userPerIdPer5min; }
        public void setUserPerIdPer5min(int v) { this.userPerIdPer5min = v; }
        public int getUserPerIdPerDay() { return userPerIdPerDay; }
        public void setUserPerIdPerDay(int v) { this.userPerIdPerDay = v; }
        public int getPerPropertyPerMin() { return perPropertyPerMin; }
        public void setPerPropertyPerMin(int v) { this.perPropertyPerMin = v; }
        public int getLikePerIdentityPerMin() { return likePerIdentityPerMin; }
        public void setLikePerIdentityPerMin(int v) { this.likePerIdentityPerMin = v; }
    }

    public static class Captcha {
        private int ttlSeconds = 300;
        private int maxPending = 10_000;

        public int getTtlSeconds() { return ttlSeconds; }
        public void setTtlSeconds(int v) { this.ttlSeconds = v; }
        public int getMaxPending() { return maxPending; }
        public void setMaxPending(int v) { this.maxPending = v; }
    }

    public static class PropertyService {
        private String baseUrl = "http://localhost:8080";
        private int timeoutSeconds = 4;
        private int cacheTtlSeconds = 60;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String v) { this.baseUrl = v; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int v) { this.timeoutSeconds = v; }
        public int getCacheTtlSeconds() { return cacheTtlSeconds; }
        public void setCacheTtlSeconds(int v) { this.cacheTtlSeconds = v; }
    }

    public boolean isDisabled() { return disabled; }
    public void setDisabled(boolean v) { this.disabled = v; }

    public String getIpHashSecret() { return ipHashSecret; }
    public void setIpHashSecret(String v) { this.ipHashSecret = v; }

    public int getEditWindowMinutes() { return editWindowMinutes; }
    public void setEditWindowMinutes(int v) { this.editWindowMinutes = v; }

    public int getBodyMinLength() { return bodyMinLength; }
    public void setBodyMinLength(int v) { this.bodyMinLength = v; }

    public int getBodyMaxLength() { return bodyMaxLength; }
    public void setBodyMaxLength(int v) { this.bodyMaxLength = v; }

    public int getDisplayNameMaxLength() { return displayNameMaxLength; }
    public void setDisplayNameMaxLength(int v) { this.displayNameMaxLength = v; }

    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit v) { this.rateLimit = v; }

    public Captcha getCaptcha() { return captcha; }
    public void setCaptcha(Captcha v) { this.captcha = v; }

    public PropertyService getPropertyService() { return propertyService; }
    public void setPropertyService(PropertyService v) { this.propertyService = v; }
}
