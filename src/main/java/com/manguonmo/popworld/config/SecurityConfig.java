package com.manguonmo.popworld.config;

import com.manguonmo.popworld.security.ratelimit.RateLimiterService;
import com.manguonmo.popworld.security.ratelimit.RateLimitingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final RateLimiterService rateLimiterService;

    public SecurityConfig(@Autowired(required = false) RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService != null ? rateLimiterService : new RateLimiterService();
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
                .addFilterBefore(new RateLimitingFilter(rateLimiterService), CsrfFilter.class)
                .csrf(csrf -> csrf
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers("/api/payment/sepay/**")
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/cart", "/cart/**", "/api/cart/**", "/checkout", "/checkout/**", "/orders", "/orders/**", "/account", "/account/**", "/profile", "/api/orders", "/api/orders/**", "/api/popnow/reserve", "/api/popnow/cancel", "/api/popnow/unbox", "/api/popnow/cabinet").authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/",true)
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
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/403")
                );

        return http.build();
    }
}
