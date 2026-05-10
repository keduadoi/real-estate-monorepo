package com.realestate.aisearch.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    public static final String PARSE_CACHE = "aiSearchParse";

    @Bean
    public CacheManager cacheManager(CacheProperties props) {
        CaffeineCacheManager mgr = new CaffeineCacheManager(PARSE_CACHE);
        mgr.setCaffeine(Caffeine.newBuilder()
                .maximumSize(props.getMaxEntries())
                .expireAfterWrite(Duration.ofMinutes(props.getTtlMinutes())));
        return mgr;
    }
}
