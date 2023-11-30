package com.zorsecyber.bouncer.webapp.configuration;

import java.io.IOException;
import java.io.Serializable;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint, Serializable {

    @Autowired
    private HttpSession httpSession;
    /**
     * Redirects to login page without authentication of the user.
     *
     * @param request
     * @param response
     * @param authException
     * @throws IOException
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if(!request.getServletPath().equals("/error") && !request.getServletPath().equals("/favicon.ico")) {
            httpSession.setAttribute("redirectUrl",request.getServletPath());
        }
        response.sendRedirect("/login");
    }
}
