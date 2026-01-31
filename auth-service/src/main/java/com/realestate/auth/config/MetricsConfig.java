package com.realestate.auth.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public AuthMetrics authMetrics(MeterRegistry registry) {
        return new AuthMetrics(registry);
    }

    public static class AuthMetrics {
        private final Counter loginTotal;
        private final Counter loginSuccess;
        private final Counter loginFailure;
        private final Counter registrationTotal;
        private final Counter registrationSuccess;
        private final Counter registrationFailure;
        private final Counter tokenRefreshTotal;
        private final Counter tokenRefreshSuccess;
        private final Counter tokenRefreshFailure;
        private final Counter passwordResetRequests;
        private final Counter accountLockouts;
        private final Timer loginLatency;
        private final Timer registrationLatency;

        public AuthMetrics(MeterRegistry registry) {
            // Login metrics
            this.loginTotal = Counter.builder("auth_login_total")
                    .description("Total login attempts")
                    .register(registry);
            this.loginSuccess = Counter.builder("auth_login_success")
                    .description("Successful login attempts")
                    .register(registry);
            this.loginFailure = Counter.builder("auth_login_failure")
                    .description("Failed login attempts")
                    .register(registry);

            // Registration metrics
            this.registrationTotal = Counter.builder("auth_registration_total")
                    .description("Total registration attempts")
                    .register(registry);
            this.registrationSuccess = Counter.builder("auth_registration_success")
                    .description("Successful registrations")
                    .register(registry);
            this.registrationFailure = Counter.builder("auth_registration_failure")
                    .description("Failed registrations")
                    .register(registry);

            // Token refresh metrics
            this.tokenRefreshTotal = Counter.builder("auth_token_refresh_total")
                    .description("Total token refresh attempts")
                    .register(registry);
            this.tokenRefreshSuccess = Counter.builder("auth_token_refresh_success")
                    .description("Successful token refreshes")
                    .register(registry);
            this.tokenRefreshFailure = Counter.builder("auth_token_refresh_failure")
                    .description("Failed token refreshes")
                    .register(registry);

            // Security metrics
            this.passwordResetRequests = Counter.builder("auth_password_reset_requests")
                    .description("Password reset requests")
                    .register(registry);
            this.accountLockouts = Counter.builder("auth_account_lockouts")
                    .description("Account lockouts due to failed attempts")
                    .register(registry);

            // Latency timers
            this.loginLatency = Timer.builder("auth_login_latency")
                    .description("Login operation latency")
                    .register(registry);
            this.registrationLatency = Timer.builder("auth_registration_latency")
                    .description("Registration operation latency")
                    .register(registry);
        }

        public void recordLoginAttempt() {
            loginTotal.increment();
        }

        public void recordLoginSuccess() {
            loginSuccess.increment();
        }

        public void recordLoginFailure() {
            loginFailure.increment();
        }

        public void recordRegistrationAttempt() {
            registrationTotal.increment();
        }

        public void recordRegistrationSuccess() {
            registrationSuccess.increment();
        }

        public void recordRegistrationFailure() {
            registrationFailure.increment();
        }

        public void recordTokenRefreshAttempt() {
            tokenRefreshTotal.increment();
        }

        public void recordTokenRefreshSuccess() {
            tokenRefreshSuccess.increment();
        }

        public void recordTokenRefreshFailure() {
            tokenRefreshFailure.increment();
        }

        public void recordPasswordResetRequest() {
            passwordResetRequests.increment();
        }

        public void recordAccountLockout() {
            accountLockouts.increment();
        }

        public Timer getLoginLatency() {
            return loginLatency;
        }

        public Timer getRegistrationLatency() {
            return registrationLatency;
        }
    }
}
