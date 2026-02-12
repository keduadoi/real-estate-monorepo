package com.realestate.backend.config;

import com.realestate.backend.analytics.ActivityTrackingInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for CORS and other web-related settings.
 *
 * When running behind Kong Gateway, CORS should be disabled here
 * as Kong handles CORS via its cors plugin.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ActivityTrackingInterceptor activityTrackingInterceptor;

    @Value("${cors.enabled:true}")
    private boolean corsEnabled;

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
    private String[] allowedOrigins;

    @Value("${storage.local.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (!corsEnabled) {
            log.info("CORS is disabled - Kong Gateway handles CORS");
            return;
        }

        log.info("CORS enabled for origins: {}", String.join(", ", allowedOrigins));
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(activityTrackingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/actuator/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
