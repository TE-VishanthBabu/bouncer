package com.zorsecyber.bouncer.webapp.service;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.zorsecyber.bouncer.webapp.constant.ApprovalStatus;
import com.zorsecyber.bouncer.webapp.constant.Constant;
import com.zorsecyber.bouncer.webapp.dao.Roles;
import com.zorsecyber.bouncer.webapp.dao.Sessions;
import com.zorsecyber.bouncer.webapp.dao.Token;
import com.zorsecyber.bouncer.webapp.dao.User;
import com.zorsecyber.bouncer.webapp.dto.EmailDto;
import com.zorsecyber.bouncer.webapp.dto.PasswordDto;
import com.zorsecyber.bouncer.webapp.dto.UserRegistrationDto;
import com.zorsecyber.bouncer.webapp.exception.EmailNotSentException;
import com.zorsecyber.bouncer.webapp.exception.UserNotFoundException;
import com.zorsecyber.bouncer.webapp.repository.RoleRepository;
import com.zorsecyber.bouncer.webapp.repository.SessionInfoRepository;
import com.zorsecyber.bouncer.webapp.repository.TokenRepository;
import com.zorsecyber.bouncer.webapp.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import lombok.extern.slf4j.Slf4j;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;

@Service
@Slf4j
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private SessionInfoRepository sessionRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private MessageSource messageSource;
    @Value("20")
    private Integer tokenExpiration;
    @Autowired
    private MailService mailService;
    @Value("${base.url}")
    private String baseUrl;

    @Value("${jwt.secret.key}")
    private String secretKey;

    /**
     * Saving user information to repository.
     *
     * @param registrationDto
     * @return user
     */
    public User save(UserRegistrationDto registrationDto){
        User user = new User();
        user.setFirstName(registrationDto.getFirstName());
        user.setLastName(registrationDto.getLastName());
        user.setName(registrationDto.getFirstName() + " " + registrationDto.getLastName());
        user.setEmail(registrationDto.getEmail());
        if(registrationDto.getPassword().equals(registrationDto.getConfirmPassword())){
            user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        }
        Roles roles=roleRepository.findByName("ROLE_USER");
        if(ObjectUtils.isEmpty(roles)) {
            user.setRoles(Arrays.asList(new Roles("ROLE_USER")));
        } else {
            user.addRole(roles);
        }
        user.setApprovalStatus(ApprovalStatus.NotApproved);
        return userRepository.save(user);
    }

    /**
     * Getting User information by email.
     *
     * @param email
     * @return user
     */
    public User findUserByEmail(String email){
        return userRepository.findByEmail(email);
    }

    /**
     * Generating Email verification token.
     *
     * @param userId
     * @param token
     */
    public void createEMailVerificationToken(Integer userId, String token) {
        Token verificationToken = this.tokenRepository.findByUserId(userId);
        if (verificationToken == null) {
            verificationToken = new Token();
        }
        verificationToken.setUserId(userId);
        verificationToken.setEmailVerificationToken(token);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, tokenExpiration);
        verificationToken.setExpiryDate(cal.getTime());
        this.tokenRepository.save(verificationToken);
    }

    /**
     * Approve account after email verification.
     *
     * @param userId
     * @return user
     */
    @Transactional
    public User verifyAccount(Integer userId) {
        User user = this.userRepository.findById(userId).orElseThrow(() -> {
            throw new UserNotFoundException(messageSource.getMessage("user.notFound", null, Locale.getDefault()));
        });
        user.setApprovalStatus(ApprovalStatus.Approved);
        Token token=tokenRepository.findByUserId(userId);
        tokenRepository.delete(token);
        return this.userRepository.save(user);
    }

    /**
     * Authenticate the user.
     *
     * @param user
     * @return user
     */
    public User authenticate(User user) {
        log.info("User Authenticated Successfully {}", user.getEmail());
        return userRepository.findById(user.getUserId()).orElseThrow(() -> {
            throw new UserNotFoundException("No user found");
        });
    }

    /**
     * Getting the user roles.
     *
     * @param roles
     * @return roles
     */
    public Collection< ? extends GrantedAuthority> mapRolesToAuthorities(Collection <Roles> roles) {
        Collection < ? extends GrantedAuthority> mapRoles = roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
        return mapRoles;
    }

    /**
     * Storing active session.
     *
     * @param user
     * @param token
     */
    public void store(User user,String token){
        Sessions sessions = new Sessions();
        sessions.setUser(user);
        sessions.setSessionId(token);
        this.sessionRepository.save(sessions);
    }

    /**
     * Delete Jwt and its details after logout.
     *
     * @param token
     */
    public void deleteToken(String token) {
        if (token != null && token.startsWith(Constant.BEARER)) {
            String jwtToken = token.substring(7);
            Sessions sessions=sessionRepository.findBySessionId(jwtToken);
            if(sessions !=null){
                sessionRepository.delete(sessions);
                log.info("JWT token deleted");
            }

        }
    }

    /**
     * Creating password reset token for the user.
     *
     * @param userId
     * @param token
     */

    public void createResetPasswordTokenForUser(Integer userId, String token ){
        Token passwordResetToken=tokenRepository.findByUserId(userId);
        if (passwordResetToken == null) {
            passwordResetToken = new Token();
        }
        passwordResetToken.setUserId(userId);
        passwordResetToken.setForgotPasswordToken(token);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, tokenExpiration);
        passwordResetToken.setExpiryDate(cal.getTime());
        this.tokenRepository.save(passwordResetToken);
    }

    /**
     * Sending mail to the respective user.
     *
     * @param user
     * @param token
     */
    public void sendMail(User user, String token) {
        try {
            EmailDto emailRequest = new EmailDto();
            emailRequest.setFrom(user.getEmail());
            emailRequest.setSubject("Reset Your Bouncer Password");
            String url = baseUrl + "resetPassword/" + token;
            emailRequest.setMessage("Click on the following link to reset your Bouncer password: <a href='" + url + "'>Reset Password</a>");
            mailService.sendMail(emailRequest.getFrom(), emailRequest.getSubject(), emailRequest.getMessage());
        } catch (EmailNotSentException ex) {
            ex.printStackTrace();
            log.error("Exception :" + ex);
        }
    }

    /**
     * Password validation.
     *
     * @param passwordDto
     * @param result
     */
    public void validatePassword(PasswordDto passwordDto,BindingResult result){
        if (!passwordDto.getNewPassword().equals(passwordDto.getConfirmPassword())) {
            result.rejectValue("passwordInvalid",null,"Passwords must match");
        }
    }

    /**
     * New password setup.
     *
     * @param userId
     * @param passwordRequest
     */
    public void setNewPassword(Integer userId, PasswordDto passwordRequest) {
        User user=this.userRepository.findById(userId).orElseThrow(() -> {
            throw new UserNotFoundException(messageSource.getMessage("user.notFound", null, Locale.getDefault()));
        });
        user.setPassword(passwordEncoder.encode(passwordRequest.getNewPassword()));
        this.userRepository.save(user);
    }

    /**
     * Getting user info by token
     *
     * @param userToken
     * @return user.
     */
    public User findUserByToken(String userToken) {
        Token token = tokenRepository.findByEmailVerificationToken(userToken);
        User user = userRepository.findById(token.getUserId()).orElseThrow(() -> {
            throw new UserNotFoundException("Could not locate account");
        });
        return user;
    }

    /**
     * Get User email
     *
     * @param request
     * @return email
     */
    public String getUserEmail(HttpServletRequest request) {
        String token = (String) request.getSession().getAttribute("Authorization");
        String jwtToken = token.substring(7);
        DecodedJWT jwt = JWT.require(HMAC512(secretKey.getBytes())).build().verify(jwtToken);
        return jwt.getSubject();
    }
}
