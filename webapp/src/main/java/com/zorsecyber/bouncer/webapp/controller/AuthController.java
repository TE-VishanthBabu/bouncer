package com.zorsecyber.bouncer.webapp.controller;

import java.util.Calendar;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.zorsecyber.bouncer.webapp.configuration.JwtTokenUtil;
import com.zorsecyber.bouncer.webapp.constant.ApprovalStatus;
import com.zorsecyber.bouncer.webapp.constant.Constant;
import com.zorsecyber.bouncer.webapp.dao.Token;
import com.zorsecyber.bouncer.webapp.dao.User;
import com.zorsecyber.bouncer.webapp.dto.LoginDto;
import com.zorsecyber.bouncer.webapp.dto.PasswordDto;
import com.zorsecyber.bouncer.webapp.repository.TokenRepository;
import com.zorsecyber.bouncer.webapp.repository.UserRepository;
import com.zorsecyber.bouncer.webapp.response.CommonResponse;
import com.zorsecyber.bouncer.webapp.response.JwtResponse;
import com.zorsecyber.bouncer.webapp.service.LicenseService;
import com.zorsecyber.bouncer.webapp.service.OAuth2Service;
import com.zorsecyber.bouncer.webapp.service.OrganizationsService;
import com.zorsecyber.bouncer.webapp.service.RolesService;
import com.zorsecyber.bouncer.webapp.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

/**
 * Authentication controller for login and logout.
 */
@Controller
@Slf4j
public class AuthController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private OAuth2Service oAuth2Service;
    
    @Autowired
    private OrganizationsService organizationService;
    
    @Autowired
    private LicenseService licenseService;
    
    @Autowired
	private RolesService roleService;



    /**
     * Display Login page and get the details from the form and add it to the model.
     *
     * @param model
     * @return login
     */
    @GetMapping("/login")
    public String showLoginPage(Model model,HttpSession session,HttpServletRequest request,HttpServletResponse response){
        if (ObjectUtils.isNotEmpty(session.getAttribute(Constant.AUTHORIZATION))) {
            //If user is already logged in, redirected to the dashboard page
            return "redirect:/";
        }
        Cookie[] cookies = request.getCookies();
        if(ObjectUtils.isNotEmpty(cookies)) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(Constant.PROVIDER)) {
                    Cookie providerCookie = new Cookie(Constant.PROVIDER, null);
                    providerCookie.setMaxAge(0);
                    providerCookie.setPath("/");
                    response.addCookie(providerCookie);
                }
            }
        }
        model.addAttribute("loginForm",new LoginDto());
        return "login";
    }

    /**
     * Login endpoint - Authenticate the user and generates JWT token
     *
     *
     * @param loginDto
     * @param model
     * @return redirect to dashboard page.
     */
    @SuppressWarnings("null")
	@PostMapping("/login/save")
    public String login(@ModelAttribute("loginForm") LoginDto loginDto, Model model, BindingResult result, HttpSession session) {

        String username = loginDto.getEmail();
        String password = loginDto.getPassword();
        User userInfo = userRepository.findByEmail(username);
        if (userInfo != null) {
        	if(userInfo.getApprovalStatus() == ApprovalStatus.Approved) {
		            boolean isPasswordMatch = passwordEncoder.matches(password, userInfo.getPassword());
		            if (!isPasswordMatch) {
		                log.error("Password mismatched {}", password);
		                result.rejectValue("password", null, "Invalid password");
		            }
            } else {
            	result.rejectValue("email", null, "Email not verified");
            }
        } else {
            result.rejectValue("email", null, "Account not found");
        }
        if(result.hasErrors()) {
            model.addAttribute("loginForm", loginDto);
            return "/login";
        }
        if(userInfo.getAuthUserId()!=null) {
            result.rejectValue("email",null,"Email is already linked with Microsoft, Please Sign in with Microsoft");
            return "/login";
        }
        User user = this.userService.authenticate(userInfo);
        // save user organization
        organizationService.saveAndUpdateUserOrganization(user);
        // check for license and assign roles
        licenseService.lookupAndAssignLicense(user);
		roleService.assignRolesBasedOnLicense(user);
        log.info("Init Token Generation");
        Collection<? extends GrantedAuthority> roles = userService.mapRolesToAuthorities(user.getRoles());
        final String token = jwtTokenUtil.generateToken(user.getUserId(), user.getEmail(), roles);
        log.info("Access Token Generated {}", token);
        userService.store(user, token);
        CommonResponse commonResponse = new CommonResponse();
        commonResponse.setData(new JwtResponse(token, roles.stream().map(role -> role.getAuthority()).collect(Collectors.toList()), user.getName()));
        commonResponse.setMessage(messageSource.getMessage("authentication.success", null, Locale.getDefault()));
        session.setAttribute(Constant.AUTHORIZATION, "Bearer " + token);
        if(!user.isLicensed()) {
            return "redirect:/unlicensed/landing";
        }
        if(session.getAttribute("redirectUrl")!=null && session.getAttribute("redirectUrl")!="/login") {
            return "redirect:" + session.getAttribute("redirectUrl");
        }
        return "redirect:/";
    }


    /**
     * Logout page - redirected to Log in.
     *
     * @return logout page
     */
    @GetMapping("/userLogout")
    public String logoutPage(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        String token = String.valueOf(session.getAttribute(Constant.AUTHORIZATION));
        String provider = String.valueOf(session.getAttribute("Provider")); //To check for microsoft logout else Normal logout
        if(provider!="null") {
            String signOutUrl = this.oAuth2Service.handleLogout();
            return "redirect:" + signOutUrl;
        } else {
            userService.deleteToken(token);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                new SecurityContextLogoutHandler().logout(request, response, auth);
                request.getSession().removeAttribute(Constant.AUTHORIZATION);
                Cookie jwtCookie = new Cookie("JSESSIONID", null);
                jwtCookie.setMaxAge(0);
                jwtCookie.setPath("/");
                response.addCookie(jwtCookie);
                request.getSession().invalidate();
            }
        }
        return "redirect:/login";
    }

    /**
     *
     * @param model
     * @return forgotPassword page
     */
    @GetMapping("/password")
    public String showPasswordPage(Model model){
        model.addAttribute("userEmail",new LoginDto());
        return "password";
    }

    /**
     * Sending reset password link to email.
     *
     * @param loginDto
     * @param model
     * @param result
     * @return password page
     */
    @PostMapping("/password/reset")
    public String resetPassword(@ModelAttribute("userEmail") LoginDto loginDto, Model model, BindingResult result){
        User user = this.userService.findUserByEmail(loginDto.getEmail());
        if(user == null) {
            result.rejectValue("email", null,
                    "Account not found");
        }
        if(result.hasErrors()) {
            model.addAttribute("userEmail", loginDto);
            return "password";
        }
        String token = UUID.randomUUID().toString();
        userService.createResetPasswordTokenForUser(user.getUserId(),token);
        userService.sendMail(user, token);
        return "redirect:/password?success";
    }

    /**
     * Redirects to resetPassword page,when selecting the reset password link received in the email.
     *
     * @param token
     * @param model
     * @return reset password page.
     */
    @GetMapping("/resetPassword/{token}")
    public String showResetPasswordPage(@PathVariable String token, Model model) {
        PasswordDto passwordDto = new PasswordDto();
        model.addAttribute("userPassword",passwordDto);
        model.addAttribute("token",token);
        passwordDto.setToken(token);
        return "resetPassword";
    }

    /**
     * Updating the password for the registered user.
     * Checks the reset password link and returns to the respective pages with Dynamic messages.
     *
     * @param passwordDto
     * @param result
     * @param model
     * @return reset password success or failure page.
     */
    @PostMapping("/updatePassword")
    public String updateNewPassword(@ModelAttribute("userPassword") PasswordDto passwordDto, BindingResult result,Model model) {
        Token passToken = tokenRepository.findByForgotPasswordToken(passwordDto.getToken());
        if (passToken == null) {
            model.addAttribute("passwordLinkNotFound","Invalid link");
            return "verificationFailure";
        }
        final Calendar cal = Calendar.getInstance();
        if (passToken.getExpiryDate().before(cal.getTime())) {
            model.addAttribute("passwordLinkExpired","Link expired");
            return "verificationFailure";
        }
        userService.validatePassword(passwordDto,result);
        if(result.hasErrors()) {
            model.addAttribute("userPassword", passwordDto);
            return "resetPassword";
        }
        userService.setNewPassword(passToken.getUserId(), passwordDto);
        model.addAttribute("passwordReset","Your password was reset successfully");
        return "verificationSuccess";
    }

    @GetMapping("/login/oAuth")
    public String getMicrosoftLogin(HttpSession session) {
        session.removeAttribute("Provider");
        return "redirect:/oAuth";
    }


}