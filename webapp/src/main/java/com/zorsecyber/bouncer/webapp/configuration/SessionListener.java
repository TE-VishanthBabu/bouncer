package com.zorsecyber.bouncer.webapp.configuration;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zorsecyber.bouncer.webapp.constant.Constant;
import com.zorsecyber.bouncer.webapp.dao.Sessions;
import com.zorsecyber.bouncer.webapp.repository.SessionInfoRepository;

@Service
@Slf4j
public class SessionListener implements HttpSessionListener {

    @Autowired
    private SessionInfoRepository sessionRepository;

    /**
     * After Session timed-out, JWT token will be deleted.
     * @param sessionEvent
     */

    @Override
    public void sessionDestroyed(HttpSessionEvent sessionEvent) {
        log.info("Session deleting");
        HttpSession httpSession = sessionEvent.getSession();
        String sessionId = String.valueOf(httpSession.getAttribute("Authorization"));
        String provider = String.valueOf(httpSession.getAttribute("Provider"));
        if(provider!=null) {
            httpSession.removeAttribute("Provider");
            httpSession.invalidate();
        }

        // Delete the token from the table using the session ID
        this.deleteTokenBySessionId(sessionId);
    }

    /**
     * Delete Session
     * @param token
     */
    public void deleteTokenBySessionId(String token) {
        if (token != null && token.startsWith(Constant.BEARER)) {
            String jwtToken = token.substring(7);
            Sessions sessions=sessionRepository.findBySessionId(jwtToken);
            if(sessions !=null) {
                sessionRepository.delete(sessions);
                log.info("JWT token deleted");
            }
        }
    }
}