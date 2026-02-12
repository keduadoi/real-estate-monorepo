package com.realestate.post.analytics;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "analytics")
public class ActivityEventMappingConfig {

    private String serviceName;
    private Map<String, String> eventMappings = new HashMap<>();
    private Kafka kafka = new Kafka();

    @Data
    public static class Kafka {
        private boolean enabled = true;
        private String topic = "user-activity-events";
    }
}
