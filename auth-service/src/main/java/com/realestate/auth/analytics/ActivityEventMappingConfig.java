package com.realestate.auth.analytics;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "analytics")
public class ActivityEventMappingConfig {

    private String serviceName;
    private Map<String, String> eventMappings = new HashMap<>();
    private Kafka kafka = new Kafka();

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public Map<String, String> getEventMappings() { return eventMappings; }
    public void setEventMappings(Map<String, String> eventMappings) { this.eventMappings = eventMappings; }

    public Kafka getKafka() { return kafka; }
    public void setKafka(Kafka kafka) { this.kafka = kafka; }

    public static class Kafka {
        private boolean enabled = true;
        private String topic = "user-activity-events";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
    }
}
