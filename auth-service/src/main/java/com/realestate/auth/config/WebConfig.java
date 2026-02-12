package com.realestate.auth.config;

import com.realestate.auth.analytics.ActivityTrackingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ActivityTrackingInterceptor activityTrackingInterceptor;

    public WebConfig(ActivityTrackingInterceptor activityTrackingInterceptor) {
        this.activityTrackingInterceptor = activityTrackingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(activityTrackingInterceptor)
                .addPathPatterns("/auth/**")
                .excludePathPatterns("/actuator/**", "/.well-known/**");
    }
}
