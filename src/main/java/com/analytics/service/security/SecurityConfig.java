package com.analytics.service.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())          // stateless JWT — CSRF не нужен
                .cors(Customizer.withDefaults())       // берёт настройки CORS из WebConfig
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // публичные ресурсы фронтенда
                        .requestMatchers("/", "/index.html", "/login.html", "/main.html", "/profile.html").permitAll()
                        .requestMatchers("/payments/webhook").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/orders/quote").permitAll()
                        .requestMatchers("/checkout.html", "/payment-return.html", "/cart.html", "/product.html").permitAll()
                        .requestMatchers("/main", "/profile").permitAll()
                        .requestMatchers("/*.css", "/*.js", "/*.png", "/*.ico", "/*.svg", "/*.html").permitAll()
                        .requestMatchers("/images/**").permitAll()
                        .requestMatchers("/catalog", "/catalog/**").permitAll()
                        .requestMatchers("/cart", "/cart/**","/cart.html").authenticated()
                        // вход/регистрация
                        .requestMatchers("/auth/**").permitAll()
                        // вся админка — только для ADMIN
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // без токена к защищённому ресурсу → 401 (а не 403), чтобы фронт понял «нужен вход»
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, e) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(CustomUserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
