package com.realestate.aisearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-search.parser")
public class ParserModeProperties {

    /** Active parser mode: "llm" or "regex". */
    private String mode = "regex";

    /** Fallback when llm fails: "regex" or "none". */
    private String fallbackOnError = "regex";

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getFallbackOnError() {
        return fallbackOnError;
    }

    public void setFallbackOnError(String fallbackOnError) {
        this.fallbackOnError = fallbackOnError;
    }
}
