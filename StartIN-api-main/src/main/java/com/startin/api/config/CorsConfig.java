package com.startin.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${startin.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        List<String> patterns = new ArrayList<>();
        patterns.add("*");
        patterns.add("https://*.vercel.app");
        patterns.add("http://localhost:*");

        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            for (String raw : allowedOrigins.split(",")) {
                String clean = raw.trim().replaceAll("/+$", "");
                if (!clean.isEmpty() && !clean.equals("*")) {
                    if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
                        patterns.add("https://" + clean);
                        patterns.add("http://" + clean);
                    } else {
                        patterns.add(clean);
                    }
                }
            }
        }

        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(patterns);
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
