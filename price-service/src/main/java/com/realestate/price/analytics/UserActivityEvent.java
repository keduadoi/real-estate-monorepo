package com.realestate.price.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
