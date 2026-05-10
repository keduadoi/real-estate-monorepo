package com.realestate.aisearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-search.anthropic")
public class AnthropicProperties {

    private String apiKey = "";
    private String baseUrl = "https://api.anthropic.com";
    private String model = "claude-haiku-4-5-20251001";
    private int maxTokens = 1024;
    private int timeoutSeconds = 8;
    private String version = "2023-06-01";

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
}
