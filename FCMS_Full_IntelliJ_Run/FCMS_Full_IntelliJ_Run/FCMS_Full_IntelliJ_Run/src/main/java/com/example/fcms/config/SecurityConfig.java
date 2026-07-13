package com.example.fcms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final GoogleLoginSuccessHandler googleLoginSuccessHandler;

    private static final String[] PUBLIC_URLS = {
            "/",
            "/login",
            "/register",
            "/forgot-password",
            "/select-role",
            "/css/**",
            "/js/**",
            "/images/**",
            "/static/**",
            "/student/**",
            "/api/auth/**",
            "/oauth2/**",
            "/login/oauth2/**"
    };


    public SecurityConfig(GoogleLoginSuccessHandler googleLoginSuccessHandler) {
        this.googleLoginSuccessHandler = googleLoginSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrfConfig -> csrfConfig.disable());

        http.authorizeHttpRequests(authConfig -> {
            authConfig.requestMatchers(PUBLIC_URLS).permitAll();
            authConfig.anyRequest().permitAll();
        });

        http.formLogin(formConfig -> formConfig.disable());
        http.httpBasic(basicConfig -> basicConfig.disable());

        http.oauth2Login(oauthConfig -> oauthConfig
                .loginPage("/login")
                .successHandler(googleLoginSuccessHandler)
        );

        return http.build();
    }
}
