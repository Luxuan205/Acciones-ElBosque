package com.accioneselbosque.configuration.config;

import com.accioneselbosque.auth.config.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.util.ReflectionTestUtils;

@TestConfiguration
public class TestJwtConfig {

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(@Value("${app.jwt.secret}") String jwtSecret) {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", jwtSecret);
        return filter;
    }
}
