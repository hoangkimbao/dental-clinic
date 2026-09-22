package com.dentalclinic.security;

import com.dentalclinic.config.RateLimitingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;

    // 100% Constructor Injection
    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitingFilter rateLimitingFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .frameOptions(frame -> frame.sameOrigin())
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdnjs.cloudflare.com https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdnjs.cloudflare.com; font-src 'self' https://fonts.gstatic.com data:; img-src 'self' data: https: blob: /uploads/; connect-src 'self' ws: wss: http://localhost:20128; frame-ancestors 'self'; base-uri 'self'; form-action 'self';")
                )
                .permissionsPolicy(permissions -> permissions
                    .policy("camera=(), microphone=(), geolocation=(self)")
                )
            )
            .authorizeHttpRequests(auth -> auth
                // Public Static Assets & Landing Page
                .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/favicon.ico", "/sitemap.xml", "/robots.txt", "/uploads/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/ws-dental/**").permitAll()

                // Public Auth & Booking & Public Coupon Endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/coupons/active").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/coupons/validate").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/appointments/book").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/appointments/*/pay-deposit").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/dentists").permitAll()

                // Public CMS Articles & Doctor Reviews
                .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/articles/**").hasAnyRole("ADMIN", "OWNER")
                .requestMatchers(HttpMethod.PUT, "/api/articles/**").hasAnyRole("ADMIN", "OWNER")
                .requestMatchers(HttpMethod.DELETE, "/api/articles/**").hasAnyRole("ADMIN", "OWNER")
                .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reviews").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/reviews/**").hasAnyRole("ADMIN", "OWNER")
                .requestMatchers(HttpMethod.POST, "/api/emr/images/upload").hasAnyRole("DENTIST", "ADMIN", "OWNER")
                .requestMatchers("/api/emr/images/**").hasAnyRole("DENTIST", "ADMIN", "OWNER", "PATIENT")
                .requestMatchers("/api/tier2-agents/**").hasAnyRole("ADMIN", "OWNER", "DENTIST", "RECEPTIONIST")
                .requestMatchers("/api/email/**").permitAll()

                // IT Team Command Center APIs strictly require ROLE_ADMIN or ROLE_OWNER
                .requestMatchers("/api/it-team/**").hasAnyRole("ADMIN", "OWNER")

                // Analytics Tracking: public non-blocking event ingestion, admin/owner summary
                .requestMatchers(HttpMethod.POST, "/api/analytics/events").permitAll()
                .requestMatchers("/api/analytics/summary").hasAnyRole("ADMIN", "OWNER")

                // Milestone 1: Customer Dental Ecosystem Public Endpoints
                .requestMatchers(HttpMethod.GET, "/api/dental-services/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/dental-products/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/dental-orders").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/dental-orders/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/warranties/**").permitAll()
                .requestMatchers("/api/loyalty/**").permitAll()
                .requestMatchers("/api/dental-ai/**").permitAll()
                .requestMatchers("/api/ai-diagnostic/**").permitAll()
                .requestMatchers("/api/forum/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/branches/**").permitAll()

                // Milestone 3: PC Desktop App CMS & Multi-Table Excel Export
                .requestMatchers("/api/cms/**").permitAll()
                .requestMatchers("/api/export/**").permitAll()

                // All other /api/** endpoints require JWT Authentication (Dashboard, Staff, EMR, Create Coupons)
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
