package com.zorsecyber.bouncer.webapp.configuration;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

import com.zorsecyber.bouncer.webapp.constant.Constant;

import java.io.IOException;

/**
 * Spring Security Configuration.
 */
@Configuration
@EnableWebSecurity
@EnableJdbcHttpSession
public class SecurityConfig {
    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;
    
    @Value("${base.url}")
    private String baseUrl;

    @Value("${spring.security.oauth2.client.registration.azure.client-id}")
    private String clientId;

    @Value("${spring.security.session.log-out-uri}")
    private String logoutUri;

    @Value("${spring.security.session.log-out-redirect-uri}")
    private String redirectUri;



    /**
     * Password encoder.
     *
     * @return BCryptPasswordEncoder
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authenticating and authorize the endpoints.
     *
     * @param http
     * @return SecurityFilterChain
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeHttpRequests((authorize) ->
                        authorize.requestMatchers("/register/**", "/js/**", "/css/**", "/img/public/**","/login/**","/registrationSuccess/**","/verificationSuccess/**","/verificationFailure/**","/password/**","/resetPassword/**","/email-verify/**","/updatePassword/**","/email-reVerify/**","/oAuth/**","/").permitAll()
                                .requestMatchers("/unlicensed/**").hasAnyAuthority("[ROLE_UNLICENSED]","[ROLE_ADMIN]","[ROLE_USER,ROLE_ADMIN]")
                                .requestMatchers("/admin/**").hasAnyAuthority("[ROLE_ADMIN]","[ROLE_USER,ROLE_ADMIN]")
                                .requestMatchers("/dashboard","/account","{batchId}/report",
                                        "{fileAttributeId}/{sha256}/fileDetails","/analyze/files","/analyze/mailboxes","/upload").hasAnyAuthority("[ROLE_USER]","[ROLE_ADMIN]","[ROLE_USER,ROLE_ADMIN]")
                                .anyRequest().authenticated().and()
                ).exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint).and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).maximumSessions(2).and().invalidSessionStrategy(((request, response) -> {
                    handleExpiredInvalidSessions(request,response);
                 }));
        return http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class).build();
    }

    /**
     * Handling expired invalid sessions.
     *
     * @param request
     * @param response
     * @throws IOException
     */
        public void handleExpiredInvalidSessions(HttpServletRequest request, HttpServletResponse response) throws IOException {
            String invalidSessionUri = "/login";
            Cookie[] cookies = request.getCookies();
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(Constant.PROVIDER)) {
                    Cookie providerCookie = new Cookie(Constant.PROVIDER, null);
                    providerCookie.setMaxAge(0);
                    providerCookie.setPath("/");
                    response.addCookie(providerCookie);
                    invalidSessionUri = logoutUri + "?post_logout_redirect_uri=" + baseUrl+redirectUri + "&client_id=" + clientId;
                    break;
                }
            }
            response.sendRedirect(invalidSessionUri);
        }

    /**
     *
     * @param auth
     * @throws Exception
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

}

