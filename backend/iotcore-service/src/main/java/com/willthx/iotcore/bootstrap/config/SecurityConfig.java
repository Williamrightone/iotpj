package com.willthx.iotcore.bootstrap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * IoTCore Service 安全設定。
 *
 * <p>iotcore-service 僅提供 internal API，由 saas-bff 注入 X-Tenant-Id header 呼叫。
 * 不處理 JWT，所有 /internal/** 與 actuator 端點直接放行。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().denyAll())
                .build();
    }
}
