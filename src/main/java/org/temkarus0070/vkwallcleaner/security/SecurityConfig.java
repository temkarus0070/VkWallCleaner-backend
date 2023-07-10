package org.temkarus0070.vkwallcleaner.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity(debug = true)
@ConfigurationProperties("application")
public class SecurityConfig {

    private String frontendUrl;

    public void setFrontendUrl(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin(frontendUrl);
        corsConfiguration.addAllowedOrigin("http://localhost:8081");
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST"));
        corsConfiguration.setAllowedHeaders(Arrays.asList("X-Requested-With", "Origin", "Content-Type", "Accept", "Authorization"));
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
        return urlBasedCorsConfigurationSource;

    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity.authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> {
                               authorizationManagerRequestMatcherRegistry.requestMatchers("/register")
                                                                         .permitAll()
                                                                         .anyRequest()
                                                                         .authenticated();
                           })

                           .csrf(AbstractHttpConfigurer::disable)
                           .oauth2Login(httpSecurityOAuth2LoginConfigurer -> {

                           })
                           .cors(httpSecurityCorsConfigurer -> httpSecurityCorsConfigurer.configurationSource(
                               corsConfigurationSource()))
                           .logout(httpSecurityLogoutConfigurer -> {

                           })
                           .oauth2ResourceServer(httpSecurityOAuth2ResourceServerConfigurer -> {
                               httpSecurityOAuth2ResourceServerConfigurer.jwt(jwtConfigurer -> {
                               });
                           })
                           .build();
    }
}
