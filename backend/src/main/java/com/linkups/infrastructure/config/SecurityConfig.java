package com.linkups.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

        @Bean
        public SessionAuthenticationFilter sessionAuthenticationFilter() {
                return new SessionAuthenticationFilter();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                log.info("Configuring SecurityFilterChain for JWT-based authentication");

                http
                        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                        .csrf(AbstractHttpConfigurer::disable)
                        .authorizeHttpRequests(authz -> authz
                                // API endpoints that don't require authentication
                                .requestMatchers("/api/auth/**").permitAll()

                                // H2 Database Console (for development only)
                                .requestMatchers("/h2-console/**").permitAll()

                                // WebSocket endpoints
                                .requestMatchers("/ws/**", "/api/chat/**").permitAll()

                                // Friends and users endpoints (you may want to secure these later)
                                .requestMatchers("/api/friends/**", "/api/users/**").permitAll()
                                .requestMatchers("/api/circles/**").permitAll()

                                // Email verification endpoints
                                .requestMatchers("/api/email/**").permitAll()

                                // Health check and actuator endpoints
                                .requestMatchers("/actuator/**").permitAll()

                                // All other API endpoints require authentication
                                .requestMatchers("/api/**").authenticated()

                                // Everything else requires authentication
                                .anyRequest().authenticated())

                        .logout(logout -> logout
                                .logoutUrl("/logout")
                                .logoutSuccessUrl("http://localhost:5173/")
                                .invalidateHttpSession(true)
                                .clearAuthentication(true)
                                .deleteCookies("JSESSIONID"))

                        .addFilterBefore(sessionAuthenticationFilter(),
                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of(
                        "http://localhost:8080",
                        "http://127.0.0.1:8080",
                        "http://localhost:5173",  // Vite dev server
                        "http://localhost:5174",  // Vite dev server
                        "http://127.0.0.1:5173",  // Vite dev server alternative
                        "https://friendavailability-production.up.railway.app",
                        "https://www.linkups.com.au"
                ));

                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }
}