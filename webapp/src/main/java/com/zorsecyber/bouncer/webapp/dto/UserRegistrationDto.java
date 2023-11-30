package com.zorsecyber.bouncer.webapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegistrationDto {

    @NotNull(message = "First name cannot be null")
    private String firstName;

    @NotNull(message = "Last name cannot be null")
    private String lastName;

    @NotNull(message = "Email cannot be null")
    @Email(regexp = "[a-z0-9._-]+@([a-z0-9.-]+\\.)+[a-z]{2,3}$", message = "Enter a valid email address")
    private String email;

    @NotNull(message = "Password should not be null")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@!#$%^&*-+=])(?=\\S+$).{8,}$",
    message = "Password must contain 8 characters with at least one upper case letter, one number and one special character.")
    private String password;

    @NotNull(message = "Password cannot be null")
    private String confirmPassword;
}
