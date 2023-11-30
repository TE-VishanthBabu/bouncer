package com.zorsecyber.bouncer.webapp.configuration;

import java.io.IOException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.zorsecyber.bouncer.webapp.constant.Constant;
import com.zorsecyber.bouncer.webapp.dao.Sessions;
import com.zorsecyber.bouncer.webapp.exception.TokenNotValidException;
import com.zorsecyber.bouncer.webapp.repository.SessionInfoRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private HttpSession session;
    @Autowired
    private SessionInfoRepository sessionRepository;
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        final String requestTokenHeader = String.valueOf(session.getAttribute("Authorization"));
        String jwtToken;

        if (requestTokenHeader != null && requestTokenHeader.startsWith(Constant.BEARER)) {
            jwtToken = requestTokenHeader.substring(7);
            Sessions sessions =sessionRepository.findBySessionId(jwtToken);
            if(sessions!=null) {
                try {
                    AuthenticationPrinciple principal = jwtTokenUtil.parseToken(jwtToken);
                    PreAuthenticatedAuthenticationToken authentication =
                            new PreAuthenticatedAuthenticationToken(principal, null,
                            		principal.getAuthority().stream().map(a -> new SimpleGrantedAuthority(a)).collect(Collectors.toList())
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (JWTVerificationException e) {
                    e.printStackTrace();
                }
            } else {
                throw new TokenNotValidException("Invalid Token");
            }
        } else {
            logger.warn("JWT Token does not begin with Bearer String");
        }
        chain.doFilter(request, response);
    }

}
