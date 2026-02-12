package com.realestate.auth.analytics;

import java.util.Map;

public class UserActivityEvent {

    private String eventId;
    private String eventType;
    private String userId;
    private String userEmail;
    private String timestamp;
    private String serviceName;
    private String httpMethod;
    private String endpoint;
    private int statusCode;
    private long durationMs;
    private Map<String, String> metadata;

    public UserActivityEvent() {
    }

    private UserActivityEvent(Builder builder) {
        this.eventId = builder.eventId;
        this.eventType = builder.eventType;
        this.userId = builder.userId;
        this.userEmail = builder.userEmail;
        this.timestamp = builder.timestamp;
        this.serviceName = builder.serviceName;
        this.httpMethod = builder.httpMethod;
        this.endpoint = builder.endpoint;
        this.statusCode = builder.statusCode;
        this.durationMs = builder.durationMs;
        this.metadata = builder.metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public static class Builder {
        private String eventId;
        private String eventType;
        private String userId;
        private String userEmail;
        private String timestamp;
        private String serviceName;
        private String httpMethod;
        private String endpoint;
        private int statusCode;
        private long durationMs;
        private Map<String, String> metadata;

        public Builder eventId(String eventId) { this.eventId = eventId; return this; }
        public Builder eventType(String eventType) { this.eventType = eventType; return this; }
        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder userEmail(String userEmail) { this.userEmail = userEmail; return this; }
        public Builder timestamp(String timestamp) { this.timestamp = timestamp; return this; }
        public Builder serviceName(String serviceName) { this.serviceName = serviceName; return this; }
        public Builder httpMethod(String httpMethod) { this.httpMethod = httpMethod; return this; }
        public Builder endpoint(String endpoint) { this.endpoint = endpoint; return this; }
        public Builder statusCode(int statusCode) { this.statusCode = statusCode; return this; }
        public Builder durationMs(long durationMs) { this.durationMs = durationMs; return this; }
        public Builder metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }

        public UserActivityEvent build() {
            return new UserActivityEvent(this);
        }
    }
}
