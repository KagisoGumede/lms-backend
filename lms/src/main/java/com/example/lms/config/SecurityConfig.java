package com.example.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/users/**").permitAll()
                .requestMatchers("/api/leaves/**").permitAll()
                .requestMatchers("/api/settings/**").permitAll()
                .requestMatchers("/api/admin/**").permitAll()
                .requestMatchers("/api/announcements/**").permitAll()
                .requestMatchers("/api/messages/**").permitAll()
                .requestMatchers("/api/teams/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}