package com.zorsecyber.bouncer.webapp.controller;

import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.zorsecyber.bouncer.webapp.configuration.JwtTokenUtil;
import com.zorsecyber.bouncer.webapp.constant.Constant;
import com.zorsecyber.bouncer.webapp.dao.User;
import com.zorsecyber.bouncer.webapp.response.CommonResponse;
import com.zorsecyber.bouncer.webapp.response.JwtResponse;
import com.zorsecyber.bouncer.webapp.service.OAuth2Service;
import com.zorsecyber.bouncer.webapp.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class OAuth2Controller {

    @Autowired
    private OAuth2Service oAuth2Service;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private MessageSource messageSource;

    /**
     * Microsoft Login.
     * @return redirect to Microsoft
     */

    @GetMapping("/oAuth")
    public String redirectToMicrosoftLogin() {
       return "redirect:"+oAuth2Service.getMicrosoftLogin();
    }

    /**
     * To get the Access token,refresh token and user information and storing into the DB.
     *
     * @param code
     * @param session
     * @return redirect to dashboard page
     */

    @GetMapping("/login/oauth2/code")
    public String handleAzureLogin(@RequestParam String code, HttpSession session, HttpServletResponse response) {
        User user = this.oAuth2Service.getToken(code);
        Collection<? extends GrantedAuthority> roles = userService.mapRolesToAuthorities(user.getRoles());
        final String token = jwtTokenUtil.generateToken(user.getUserId(), user.getEmail(), roles);
        log.info("Access Token Generated {}", token);
        userService.store(user, token);
        CommonResponse commonResponse = new CommonResponse();
        commonResponse.setData(new JwtResponse(token, roles.stream().map(role -> role.getAuthority()).collect(Collectors.toList()), user.getName()));
        commonResponse.setMessage(messageSource.getMessage("authentication.success", null, Locale.getDefault()));
        session.setAttribute("Authorization","Bearer " + token);
        session.setAttribute(Constant.PROVIDER,Constant.PROVIDER_INFO);
        Cookie jwtCookie = new Cookie(Constant.PROVIDER,Constant.PROVIDER_INFO);
        jwtCookie.setMaxAge(7 * 24 * 60 * 60);
        jwtCookie.setPath("/");
        response.addCookie(jwtCookie);
        if(!user.isLicensed()) {
            return "redirect:/unlicensed/landing";
        }
        if(session.getAttribute("redirectUrl")!=null && session.getAttribute("redirectUrl")!="/login") {
            return "redirect:" + session.getAttribute("redirectUrl");
        }
        return "redirect:/";
    }

    @GetMapping("/microsoftLogout")
    public String handleMicroSoftLogout(HttpSession session, HttpServletResponse response, HttpServletRequest request) {
        String token = String.valueOf(session.getAttribute("Authorization"));
        userService.deleteToken(token);
        session.removeAttribute("Provider");
        Cookie jwtCookie = new Cookie("JSESSIONID", null);
        jwtCookie.setMaxAge(0);
        jwtCookie.setPath("/");
        response.addCookie(jwtCookie);
        request.getSession().invalidate();
        return "redirect:/login";
    }
}
