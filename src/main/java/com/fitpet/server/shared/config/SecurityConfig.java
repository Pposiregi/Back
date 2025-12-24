package com.fitpet.server.shared.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private static final String[] SWAGGER_WHITELIST = {
        "/swagger-ui.html",
        "/swagger-ui/index.html",
        "/swagger-ui/**",
        "/v3/api-docs",
        "/v3/api-docs/**"
    };

    private static final String[] PERMIT_URL_ARRAY = {
            "/auth/**",
            "/users/**",
            "/index.html",
            "/error",
            "/daily/**",
            "/body-histories/**",
            "/actuator/**",
            "/gps/**",
            "/report/**",
            "/meal/**",
            "/alram/**",
            "/pets/**",
            "/missions/**",
            "/badges/**",
            "/terms/**",
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                .requestMatchers(SWAGGER_WHITELIST).permitAll()
                .requestMatchers(HttpMethod.GET, "/swagger-ui/swagger-config").permitAll()
                .requestMatchers(PERMIT_URL_ARRAY).permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
            .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
            .requestMatchers(SWAGGER_WHITELIST);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        // GCP HTTPS 연결을 위하여 추가설정
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "https://back-847805723578.europe-west1.run.app",
                "http://localhost:3000",
                "http://localhost:8080"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
