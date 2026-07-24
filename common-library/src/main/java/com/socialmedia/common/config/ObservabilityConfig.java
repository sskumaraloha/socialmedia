package com.socialmedia.common.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tags every metric emitted by a service with its application name so a single
 * Prometheus/Grafana stack can distinguish which of the 13 services a metric came from.
 */
@Configuration
@ConditionalOnClass(MeterRegistry.class)
public class ObservabilityConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonMetricsTags(
            @Value("${spring.application.name:service}") String appName) {
        return registry -> registry.config().commonTags("application", appName);
    }
}
