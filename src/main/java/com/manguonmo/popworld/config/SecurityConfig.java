package com.manguonmo.popworld.config;

import com.manguonmo.popworld.repository.UserRepository;
import com.manguonmo.popworld.security.filter.Admin2faFilter;
import com.manguonmo.popworld.security.ratelimit.RateLimiterService;
import com.manguonmo.popworld.security.ratelimit.RateLimitingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final RateLimiterService rateLimiterService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Value("${app.security.remember-me.key:${REMEMBER_ME_KEY:popworld-remember-me-secret-key-2026}}")
    private String rememberMeKey;

    public SecurityConfig(java.util.Optional<RateLimiterService> rateLimiterService,
                          java.util.Optional<UserDetailsService> userDetailsService,
                          java.util.Optional<UserRepository> userRepository) {
        this.rateLimiterService = rateLimiterService != null && rateLimiterService.isPresent()
                ? rateLimiterService.get()
                : new RateLimiterService();
        this.userDetailsService = userDetailsService != null ? userDetailsService.orElse(null) : null;
        this.userRepository = userRepository != null ? userRepository.orElse(null) : null;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler requestHandler = new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
                .addFilterBefore(new RateLimitingFilter(rateLimiterService), CsrfFilter.class);

        if (userRepository != null) {
            http.addFilterAfter(new Admin2faFilter(userRepository), org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);
        }

        http
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; " +
                                        "img-src 'self' data: blob: https://res.cloudinary.com https://*.popmart.com https://*.unsplash.com https://*.bigcommerce.com https://arttoyfamilia.com https://api.qrserver.com https://qr.sepay.vn https://*.sepay.vn https://*.vietqr.io https://placehold.co https://*.googleusercontent.com; " +
                                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                                        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com https://fonts.googleapis.com; " +
                                        "font-src 'self' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com https://fonts.gstatic.com data:; " +
                                        "connect-src 'self' https://qr.sepay.vn https://*.vietqr.io https://api.qrserver.com; " +
                                        "frame-ancestors 'none';")
                        )
                        .referrerPolicy(referrer -> referrer
                                .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                )
                .csrf(csrf -> csrf
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers("/api/payment/sepay/**")
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/cart", "/cart/**", "/api/cart/**", "/checkout", "/checkout/**", "/orders", "/orders/**", "/my-orders", "/my-orders/**", "/account", "/account/**", "/profile", "/api/orders", "/api/orders/**", "/api/popnow/**", "/popnow/pick/**", "/popnow/cabinet", "/popnow/reveal/**", "/popnow/checkout/**", "/reviews", "/reviews/**", "/api/reviews", "/api/reviews/**", "/wishlist", "/wishlist/**", "/api/wishlist", "/api/wishlist/**").authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", false)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionFixation(sessionFixation -> sessionFixation.changeSessionId())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        if (userDetailsService != null) {
            String effectiveKey = (rememberMeKey != null && !rememberMeKey.trim().isBlank())
                    ? rememberMeKey.trim()
                    : "popworld-remember-me-secret-key-2026";
            http.rememberMe(remember -> remember
                    .userDetailsService(userDetailsService)
                    .key(effectiveKey)
                    .tokenValiditySeconds(86400 * 14)
                    .rememberMeParameter("remember-me")
            );
        }

        http.exceptionHandling(ex -> ex
                .accessDeniedPage("/403")
        );

        return http.build();
    }
}
