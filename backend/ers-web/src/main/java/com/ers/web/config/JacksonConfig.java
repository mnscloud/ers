package com.ers.web.config;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Several controllers still serialize JPA entities directly rather than mapping to response DTOs
 * (a scaffold shortcut - see application.yml's open-in-view note). This module lets Jackson handle
 * Hibernate lazy proxies without blowing up on uninitialized associations.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Hibernate6Module hibernate6Module() {
        return new Hibernate6Module();
    }
}
