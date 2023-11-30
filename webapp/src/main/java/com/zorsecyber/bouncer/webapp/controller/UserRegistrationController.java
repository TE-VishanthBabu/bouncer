package com.zorsecyber.bouncer.webapp.controller;

import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.zorsecyber.bouncer.webapp.dao.Token;
import com.zorsecyber.bouncer.webapp.dao.User;
import com.zorsecyber.bouncer.webapp.dto.EmailDto;
import com.zorsecyber.bouncer.webapp.dto.UserRegistrationDto;
import com.zorsecyber.bouncer.webapp.exception.EmailNotSentException;
import com.zorsecyber.bouncer.webapp.repository.TokenRepository;
import com.zorsecyber.bouncer.webapp.response.CommonResponse;
import com.zorsecyber.bouncer.webapp.service.MailService;
import com.zorsecyber.bouncer.webapp.service.UserService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;


/**
 * Controller for register.
 */
@Controller
@Slf4j
public class UserRegistrationController {
	private String verificationEmailSubject = "Bouncer Email Verification";

    @Autowired
    private UserService userService;

    @Autowired
    private MailService mailService;

    @Value("${base.url}")
    private String baseUrl;

    @Autowired
    private MessageSource messageSource;
    @Autowired
    private TokenRepository tokenRepository;

    /**
     *Get Register page and get the details from the form and add it to the model.
     *
     * @param model
     * @return register page.
     */
    @GetMapping("/register")
    public String showRegistrationForm(Model model){
        UserRegistrationDto userRegistrationDto= new UserRegistrationDto();
        model.addAttribute("user",userRegistrationDto);
        return "register";
    }

    /**
     * Register endpoint - It registers and generates email verification mail to the user.
     *
     * @param registrationDto
     * @param result
     * @param model
     * @return register
     */
    @PostMapping("/register/save")
    public String registerUserAccount(@ModelAttribute("user") @Valid UserRegistrationDto registrationDto, BindingResult result, Model
                                      model) {
        User existingUser =userService.findUserByEmail(registrationDto.getEmail());
        if(existingUser != null && existingUser.getEmail() != null && !existingUser.getEmail().isEmpty()){
            result.rejectValue("email", null,
                    "Account with this email already exists");
        } else if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            result.rejectValue("confirmPassword",null,
                    "Passwords must match!");
        }
        if(result.hasErrors()) {
            model.addAttribute("user", registrationDto);
            return "/register";
        }
        User user=userService.save(registrationDto);
        String token = UUID.randomUUID().toString();
        this.userService.createEMailVerificationToken(user.getUserId(),token);
        try {
            String url = baseUrl + "email-verify/" + token;
            mailService.sendMail(user.getEmail(), verificationEmailSubject, verificationEmailBody(user, url));
        } catch (EmailNotSentException ex) {
            ex.printStackTrace();
            log.error("Exception :" + ex);
        }
        model.addAttribute("registerSuccess","Email Verification");
        return "registrationSuccess";
    }

    /**
     * Email verification
     *
     * @param token
     * @return verification success or failure
     */
    @GetMapping("/email-verify/{token}")
    public String verifyEmail(@PathVariable String token, Model model){
        final Token emailVerificationToken = tokenRepository.findByEmailVerificationToken(token);
        if (emailVerificationToken == null) {
            model.addAttribute("notFound","Invalid email verification link");
            return "verificationFailure";
        }
        Integer userId = emailVerificationToken.getUserId();
        final Calendar cal = Calendar.getInstance();
        if (emailVerificationToken.getExpiryDate().before(cal.getTime())) {
            EmailDto emailDto = new EmailDto();
            model.addAttribute("expired","Email verification link expired");
            model.addAttribute("emailVerify",emailDto);
            model.addAttribute("token",token);
            emailDto.setToken(token);
            return "verificationFailure";
        }
        if(userId!=null) {
            this.userService.verifyAccount(userId);
            CommonResponse response = new CommonResponse();
            response.setMessage(messageSource.getMessage("account.verified", null, Locale.getDefault()));
        }
        model.addAttribute("verified","Your email has been verified. " +
                "Click the button below to proceed to login");
        return "verificationSuccess";
    }

    /**
     * Re-send email verification link to the user email.
     *
     * @param emailDto
     * @param model
     * @return email re-verification link
     */
    @GetMapping("/email-reVerify")
    public String emailReverification(@ModelAttribute("emailVerify") EmailDto emailDto,Model model){
        User user=userService.findUserByToken(emailDto.getToken());
        String token = UUID.randomUUID().toString();
        this.userService.createEMailVerificationToken(user.getUserId(),token);
        try {
            String url = baseUrl + "email-verify/" + token;
            mailService.sendMail(user.getEmail(), verificationEmailSubject, verificationEmailBody(user, url));
        } catch (EmailNotSentException ex) {
            ex.printStackTrace();
            log.error("Exception :" + ex);
        }
        model.addAttribute("emailVerify", "Email Verification");
        return "registrationSuccess";
    }



    @GetMapping("/registrationSuccess")
    public String showRegistrationSuccessForm(){
        return "registrationSuccess";
    }

    @GetMapping("/verificationSuccess")
    public String showVerificationSuccess(){
        return "verificationSuccess";
    }

    @GetMapping("/verificationFailure")
    public String showVerificationFailurePage(){
        return "verificationFailure";
    }
    
    private String verificationEmailBody(User user, String url) {
    	return "<br> <strong> Dear " + user.getFirstName() + ",</strong> <br> Click on the following link to verify your email address: <a href='" + url + "'>Verify</a>";
    }


}
