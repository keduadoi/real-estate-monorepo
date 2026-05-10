package com.realestate.aisearch.service;

import com.realestate.aisearch.config.CacheConfig;
import com.realestate.aisearch.config.ParserModeProperties;
import com.realestate.aisearch.dto.AiSearchParseResponse;
import com.realestate.aisearch.parser.ParseResult;
import com.realestate.aisearch.parser.ParserMode;
import com.realestate.aisearch.parser.QueryParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
public class AiSearchService {

    private final QueryParser regexParser;
    private final QueryParser llmParser;
    private final ParserModeProperties config;
    private final Cache cache;

    public AiSearchService(@Qualifier("regexQueryParser") QueryParser regexParser,
                           @Qualifier("llmQueryParser") QueryParser llmParser,
                           ParserModeProperties config,
                           CacheManager cacheManager) {
        this.regexParser = regexParser;
        this.llmParser = llmParser;
        this.config = config;
        this.cache = cacheManager.getCache(CacheConfig.PARSE_CACHE);
    }

    public AiSearchParseResponse parse(String query, String locale) {
        String trimmedQuery = query == null ? "" : query.trim();
        String localeKey = locale == null ? "" : locale.trim();
        ParserMode requestedMode = ParserMode.fromString(config.getMode());

        String cacheKey = cacheKey(trimmedQuery, localeKey, requestedMode);
        AiSearchParseResponse cached = cache != null ? cache.get(cacheKey, AiSearchParseResponse.class) : null;
        if (cached != null) {
            return new AiSearchParseResponse(
                    cached.originalQuery(),
                    cached.filters(),
                    cached.chips(),
                    cached.warnings(),
                    cached.parserMode(),
                    cached.parserLatencyMs(),
                    true
            );
        }

        long start = System.nanoTime();
        ParserMode usedMode = requestedMode;
        ParseResult result;

        try {
            result = pickParser(requestedMode).parse(trimmedQuery, localeKey);
        } catch (RuntimeException e) {
            if (requestedMode == ParserMode.LLM
                    && "regex".equalsIgnoreCase(config.getFallbackOnError())) {
                log.warn("LLM parser failed; falling back to regex: {}", e.getMessage());
                usedMode = ParserMode.REGEX;
                result = regexParser.parse(trimmedQuery, localeKey);
            } else {
                throw e;
            }
        }

        long ms = (System.nanoTime() - start) / 1_000_000;
        AiSearchParseResponse response = new AiSearchParseResponse(
                trimmedQuery,
                result.filters(),
                result.chips(),
                result.warnings(),
                usedMode.wireValue(),
                ms,
                false
        );
        if (cache != null) {
            cache.put(cacheKey, response);
        }
        return response;
    }

    private QueryParser pickParser(ParserMode mode) {
        return mode == ParserMode.LLM ? llmParser : regexParser;
    }

    private static String cacheKey(String query, String locale, ParserMode mode) {
        return mode.wireValue() + "|" + locale.toLowerCase(Locale.ROOT) + "|"
                + query.toLowerCase(Locale.ROOT);
    }
}
