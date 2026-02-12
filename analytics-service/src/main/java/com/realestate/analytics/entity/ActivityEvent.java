package com.realestate.analytics.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "activity_events")
public class ActivityEvent {

    @Id
    private String id;

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
