package com.brifo.server.auth.dev

import com.brifo.server.auth.security.JwtAuthenticationFilter
import com.brifo.server.auth.security.RestAccessDeniedHandler
import com.brifo.server.auth.security.RestAuthenticationEntryPoint
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@Profile("dev")
@ConditionalOnProperty(prefix = "app.dev-auth", name = ["enabled"], havingValue = "true")
@EnableConfigurationProperties(DevAuthProperties::class)
class DevAuthSecurityConfig {
    @Bean
    @Order(1)
    fun devAuthSecurityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthenticationFilter,
        authenticationEntryPoint: RestAuthenticationEntryPoint,
        accessDeniedHandler: RestAccessDeniedHandler,
    ): SecurityFilterChain =
        http
            .securityMatcher("/api/dev/**")
            .csrf { it.disable() }
            .cors { }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }.formLogin { it.disable() }
            .httpBasic { it.disable() }
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint)
                it.accessDeniedHandler(accessDeniedHandler)
            }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.POST, "/api/dev/signup").permitAll()
                it
                    .requestMatchers(HttpMethod.POST, "/api/dev/onboarding/complete")
                    .hasAuthority(JwtAuthenticationFilter.SIGNUP_AUTHORITY)
                it
                    .requestMatchers(HttpMethod.POST, "/api/dev/batches/**")
                    .permitAll()
                it.anyRequest().denyAll()
            }.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
}
