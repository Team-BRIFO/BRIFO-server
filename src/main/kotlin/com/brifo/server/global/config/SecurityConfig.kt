package com.brifo.server.global.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties::class)
class SecurityConfig(
	private val corsProperties: CorsProperties,
) {
	@Bean
	fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
		http
			.csrf { it.disable() }
			.cors { }
			.sessionManagement {
				it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			}
			.formLogin { it.disable() }
			.httpBasic { it.disable() }
			.authorizeHttpRequests {
				it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				it.requestMatchers(
					"/swagger-ui/**",
					"/v3/api-docs/**",
					"/actuator/health",
					"/actuator/info",
				).permitAll()
				it.anyRequest().permitAll()
			}
			.build()

	@Bean
	fun corsConfigurationSource(): CorsConfigurationSource {
		val configuration =
			CorsConfiguration().apply {
				allowedOrigins = corsProperties.allowedOrigins
				allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				allowedHeaders = listOf("Authorization", "Content-Type")
				allowCredentials = false
			}

		return UrlBasedCorsConfigurationSource().apply {
			registerCorsConfiguration("/**", configuration)
		}
	}
}
