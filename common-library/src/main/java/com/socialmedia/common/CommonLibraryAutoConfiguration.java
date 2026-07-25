package com.socialmedia.common;

import com.socialmedia.common.config.CorrelationIdFilterConfig;
import com.socialmedia.common.config.ObservabilityConfig;
import com.socialmedia.common.config.OpenApiConfig;
import com.socialmedia.common.exception.GlobalExceptionHandler;
import com.socialmedia.common.security.JwtValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Entry point that every service picks up automatically (via
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)
 * simply by depending on common-library — no manual @ComponentScan needed. common-library
 * classes live outside every service's own base package, so Spring Boot's default
 * component scan never finds them on its own - anything meant to be a bean must be
 * listed here explicitly.
 */
@AutoConfiguration
@Import({
        GlobalExceptionHandler.class,
        OpenApiConfig.class,
        ObservabilityConfig.class,
        CorrelationIdFilterConfig.class,
        JwtValidator.class
})
public class CommonLibraryAutoConfiguration {
}
