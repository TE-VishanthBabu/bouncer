package com.zorsecyber.bouncer.webapp.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;

import com.zorsecyber.bouncer.webapp.dao.Sessions;
import com.zorsecyber.bouncer.webapp.repository.SessionInfoRepository;

import java.io.IOException;

@Slf4j
@Configuration
public class SessionTimeoutInterceptor implements HandlerInterceptor {

    private final SessionInfoRepository sessionRepository;

    private final int sessionTimeout;

    public SessionTimeoutInterceptor(SessionInfoRepository sessionRepository, int sessionTimeout) {
        this.sessionRepository = sessionRepository;
        this.sessionTimeout = sessionTimeout;
    }

    /**
     * To handle Session timeout
     *
     * @param request
     * @param response
     * @param handler
     * @return boolean
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        log.info("Session timeout refreshed");

        String token = (String) request.getSession().getAttribute("Authorization");
        if(token!=null) {
            String jwtToken = token.substring(7);
            if (!isSessionValid(jwtToken)) {
                request.getSession().removeAttribute("Authorization");
                request.getSession().invalidate();
                // Redirect to the login page
                response.sendRedirect("/login");
                return false;
            }
        }
        request.getSession().setMaxInactiveInterval(sessionTimeout); // 1Hour in seconds
        return true;
    }

    /**
     * Check for the session present in DB
     * @param token
     * @return boolean
     */
    private boolean isSessionValid(String token) {
        Sessions sessions = sessionRepository.findBySessionId(token);
        return ObjectUtils.isNotEmpty(sessions);
    }


}
