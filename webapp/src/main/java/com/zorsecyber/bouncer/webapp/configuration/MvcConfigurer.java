package com.zorsecyber.bouncer.webapp.configuration;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.zorsecyber.bouncer.webapp.repository.SessionInfoRepository;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class MvcConfigurer implements WebMvcConfigurer {

    @Autowired
    private SessionInfoRepository sessionRepository;

    @Value("${session.timeout}")
    private int sessionTimer;

    /**
     * Return messages from the response.
     *
     * @return message
     */
    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames("classpath:application-message");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setDefaultLocale(Locale.ENGLISH);
        return messageSource;
    }

    /**
     *  Rest template
     * @return rest template
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Adding refresh Session timeout to all URLS.
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SessionTimeoutInterceptor(sessionRepository, sessionTimeout()))
                .addPathPatterns("/**")// Apply the interceptor to all URLs
                .excludePathPatterns("/register/**", "/js/**", "/css/**", "/img/public/**","/login/**","/registrationSuccess/**"
                        ,"/verificationSuccess/**","/verificationFailure/**","/password/**","/resetPassword/**","/email-verify/**","/updatePassword/**","/email-reVerify/**","/oAuth/**");
    }

    @Bean
    public int sessionTimeout() { return sessionTimer; }
}
