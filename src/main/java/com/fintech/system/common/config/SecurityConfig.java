package com.fintech.system.common.config;

import com.fintech.system.common.JWT.JwtAuthenticationFilter;
import com.fintech.system.common.OAuth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 1. Precise paths for Swagger & OpenAPI
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // only these specific endpoints are public
                        .requestMatchers(HttpMethod.POST, "/api/auth/users/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/users/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/users/verify-email/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/users").hasRole("ADMIN")  // only admin can get all users
                        .requestMatchers(HttpMethod.DELETE, "/api/auth/users/**").hasRole("ADMIN")  // only admin can delete
                        .requestMatchers(HttpMethod.GET, "/api/auth/users/{id}").hasAnyRole("ADMIN", "USER")  // both can get by id
                        // everything else requires JWT
                        .anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
