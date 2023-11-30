package com.zorsecyber.bouncer.webapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginDto {

    @NotNull(message = "Email should not null")
    @Email(regexp = "[a-z0-9._-]+@([a-z0-9.-]+\\.)+[a-z]{2,3}$",message = "Enter valid Email address")
    private String email;

    @NotNull(message = "Password should not be null")
    private String password;
}
