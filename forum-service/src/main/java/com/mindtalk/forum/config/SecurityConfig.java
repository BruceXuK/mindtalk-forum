package com.mindtalk.forum.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（REST API 无状态）
            .csrf(csrf -> csrf.disable())
            // 无状态会话
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 请求授权
            .authorizeHttpRequests(auth -> auth
                // Knife4j / Swagger 放行
                .requestMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**",
                        "/swagger-resources/**", "/swagger-ui/**").permitAll()
                // 认证接口放行
                .requestMatchers("/auth/**").permitAll()
                // Actuator 健康检查放行
                .requestMatchers("/actuator/health").permitAll()
                // 公开读取
                .requestMatchers(HttpMethod.GET,
                        "/posts/**", "/comments/**", "/users/*/profile", "/categories/**", "/tags/**",
                        "/search/**", "/rss/**").permitAll()
                // 浏览计数（匿名）
                .requestMatchers(HttpMethod.POST, "/posts/*/view").permitAll()
                // 管理接口需要 ADMIN 角色
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // 其余需要认证
                .anyRequest().authenticated()
            )
            // JWT 过滤器插入在 UsernamePasswordAuthenticationFilter 之前
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
