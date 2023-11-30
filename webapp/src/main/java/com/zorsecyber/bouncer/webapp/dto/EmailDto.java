package com.zorsecyber.bouncer.webapp.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailDto {

    private List<String> recipients;
    private String subject;
    private String message;
    private String from;
    private List<String> bccRecipients;
    private String token;
}
