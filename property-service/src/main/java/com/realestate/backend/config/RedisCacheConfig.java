package com.realestate.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.CacheStatisticsCollector;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis-backed cache for read-heavy public endpoints.
 *
 * Disabled when {@code app.cache.enabled=false} (env {@code CACHE_ENABLED=false}):
 * the entire config — including {@code @EnableCaching} — drops out, so {@code @Cacheable}
 * annotations on services become silent no-ops and the app runs without Redis.
 */
@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = true)
public class RedisCacheConfig implements CachingConfigurer {

    public static final String CACHE_PROPERTIES_LIST = "properties:list";
    public static final String CACHE_CITIES = "properties:cities";

    @Value("${app.cache.properties-list-ttl-seconds:60}")
    private long listTtlSeconds;

    @Value("${app.cache.cities-ttl-seconds:3600}")
    private long citiesTtlSeconds;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(buildObjectMapper());

        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> perCacheConfigs = Map.of(
                CACHE_PROPERTIES_LIST, baseConfig.entryTtl(Duration.ofSeconds(listTtlSeconds)),
                CACHE_CITIES,          baseConfig.entryTtl(Duration.ofSeconds(citiesTtlSeconds))
        );

        log.info("Redis cache enabled — listings TTL={}s, cities TTL={}s",
                listTtlSeconds, citiesTtlSeconds);

        // Stats are collected at the writer level — passing a CacheStatisticsCollector here
        // is what makes hits/misses/puts visible to Spring Boot's RedisCacheMetrics binder.
        RedisCacheWriter writer = RedisCacheWriter
                .nonLockingRedisCacheWriter(connectionFactory)
                .withStatisticsCollector(CacheStatisticsCollector.create());

        return RedisCacheManager.builder(writer)
                .cacheDefaults(baseConfig.entryTtl(Duration.ofSeconds(60)))
                .withInitialCacheConfigurations(perCacheConfigs)
                // Eagerly create these caches at startup so Micrometer's CacheMetricsRegistrar
                // can attach cache.* meters before the first request lands.
                .initialCacheNames(perCacheConfigs.keySet())
                .transactionAware()
                .build();
    }

    /**
     * Swallow Redis failures so a cache outage degrades gracefully to direct DB calls
     * instead of failing requests.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Redis GET failed (cache='{}', key='{}'): {}", cache.getName(), key, e.getMessage());
            }
            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Redis PUT failed (cache='{}', key='{}'): {}", cache.getName(), key, e.getMessage());
            }
            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Redis EVICT failed (cache='{}', key='{}'): {}", cache.getName(), key, e.getMessage());
            }
            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Redis CLEAR failed (cache='{}'): {}", cache.getName(), e.getMessage());
            }
        };
    }

    /**
     * Jackson configured for cache values:
     * - JavaTimeModule so LocalDateTime / OffsetDateTime round-trip cleanly.
     * - Default typing so generic containers like {@code PageResponse<PropertyDTO>} deserialize
     *   back to the right concrete types via embedded {@code @class} hints.
     */
    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        PolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        mapper.activateDefaultTyping(
                validator,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY);
        return mapper;
    }
}
