package com.brifo.server.global.config

import com.brifo.server.auth.config.SignupTokenCookieProperties
import com.brifo.server.auth.security.JwtAuthenticationFilter
import com.brifo.server.auth.security.RestAccessDeniedHandler
import com.brifo.server.auth.security.RestAuthenticationEntryPoint
import com.brifo.server.auth.security.SignupCsrfFilter
import com.brifo.server.auth.security.SignupTokenCookieManager
import com.brifo.server.auth.service.JwtTokenProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(
    CorsProperties::class,
    JwtProperties::class,
    KakaoProperties::class,
    NaverProperties::class,
    SignupTokenCookieProperties::class,
)
class SecurityConfig(
    private val corsProperties: CorsProperties,
) {
    @Bean
    fun jwtAuthenticationFilter(
        jwtTokenProvider: JwtTokenProvider,
        signupTokenCookieManager: SignupTokenCookieManager,
    ): JwtAuthenticationFilter = JwtAuthenticationFilter(jwtTokenProvider, signupTokenCookieManager)

    @Bean
    fun signupCsrfFilter(
        signupTokenCookieManager: SignupTokenCookieManager,
        accessDeniedHandler: RestAccessDeniedHandler,
    ): SignupCsrfFilter = SignupCsrfFilter(signupTokenCookieManager, accessDeniedHandler)

    @Bean
    fun authenticationEntryPoint(objectMapper: ObjectMapper): RestAuthenticationEntryPoint =
        RestAuthenticationEntryPoint(objectMapper)

    @Bean
    fun accessDeniedHandler(objectMapper: ObjectMapper): RestAccessDeniedHandler =
        RestAccessDeniedHandler(objectMapper)

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthenticationFilter,
        signupCsrfFilter: SignupCsrfFilter,
        authenticationEntryPoint: RestAuthenticationEntryPoint,
        accessDeniedHandler: RestAccessDeniedHandler,
    ): SecurityFilterChain =
        http
            // Cookie-authenticated signup requests are protected by SignupCsrfFilter.
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
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/actuator/health",
                        "/actuator/info",
                    ).permitAll()
                it
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/login/kakao",
                        "/api/auth/login/naver",
                        "/api/auth/reissue",
                    ).permitAll()
                it
                    .requestMatchers(
                        HttpMethod.PATCH,
                        "/api/onboarding/profile",
                    ).hasAuthority(JwtAuthenticationFilter.SIGNUP_AUTHORITY)
                it
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/onboarding/complete",
                    ).hasAuthority(JwtAuthenticationFilter.SIGNUP_AUTHORITY)
                it
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/policies",
                        "/api/policies/*",
                        "/api/stocks",
                    ).hasAnyAuthority(
                        JwtAuthenticationFilter.ACCESS_AUTHORITY,
                        JwtAuthenticationFilter.SIGNUP_AUTHORITY,
                    )
                it
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/users/me/policies",
                    ).hasAnyAuthority(
                        JwtAuthenticationFilter.ACCESS_AUTHORITY,
                        JwtAuthenticationFilter.SIGNUP_AUTHORITY,
                    )
                it.anyRequest().hasAuthority(JwtAuthenticationFilter.ACCESS_AUTHORITY)
            }.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(signupCsrfFilter, JwtAuthenticationFilter::class.java)
            .build()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                allowedOrigins = corsProperties.allowedOrigins
                allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                allowedHeaders = listOf("Authorization", "Content-Type", SignupTokenCookieManager.CSRF_HEADER_NAME)
                exposedHeaders = listOf(SignupTokenCookieManager.CSRF_HEADER_NAME)
                allowCredentials = true
            }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
