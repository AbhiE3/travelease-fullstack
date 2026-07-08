package com.travelease.backend.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelease.backend.security.JwtAuthFilter;
import com.travelease.backend.shared.dto.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/api/auth/register", "/api/auth/login", "/health", "/h2-console/**",
                            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/error"
                    ).permitAll();

                    // Must be declared before the public browsing wildcard below: these two
                    // depend on the current authenticated user (busbooking's SecurityUtil),
                    // despite living under /api/schedules/**.
                    auth.requestMatchers(HttpMethod.GET,
                            "/api/schedules/search/history",
                            "/api/schedules/search/suggestions").authenticated();

                    // Public traveler-facing browsing/search (no login required).
                    auth.requestMatchers(HttpMethod.GET,
                            "/api/buses/**", "/api/buses",
                            "/api/routes/**", "/api/routes",
                            "/api/schedules/**", "/api/schedules",
                            "/api/seats/**", "/api/seats").permitAll();
                    auth.requestMatchers("/api/bookings/ticket/verify/**").permitAll();

                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        // Written directly in the app's own ApiResponse/ApiError shape rather
                        // than response.sendError(...): sendError delegates to Spring Boot's
                        // default /error handling, whose JSON body (timestamp/status/error/
                        // message/path, with "error" as a plain String) does not deserialize
                        // as ApiResponse (whose "error" component is the ApiError object) -
                        // every other 4xx/5xx path in this API goes through
                        // GlobalExceptionHandler and returns ApiResponse, so the pre-
                        // authentication case is made consistent with that same contract.
                        (request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    ApiResponse.error("UNAUTHORIZED", "Authentication is required to access this resource")));
                        }
                ))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Origin",
                "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
