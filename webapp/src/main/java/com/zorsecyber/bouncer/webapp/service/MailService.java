package com.zorsecyber.bouncer.webapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {

    @Value("${email.id}")
    private String from;

    private final JavaMailSender javaMailSender;

    @Autowired
    public MailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public Boolean sendMail(String to, String sub, String body) {
        try {
            MimeMessage mime = this.javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(sub);
            helper.setText(body, true);
            javaMailSender.send(mime);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
}