package com.realestate.aisearch.parser;

public enum ParserMode {
    REGEX,
    LLM;

    public String wireValue() {
        return name().toLowerCase();
    }

    public static ParserMode fromString(String value) {
        if (value == null) {
            return REGEX;
        }
        String trimmed = value.trim().toLowerCase();
        return switch (trimmed) {
            case "llm" -> LLM;
            case "regex" -> REGEX;
            default -> throw new IllegalArgumentException("Unknown parser mode: " + value);
        };
    }
}
