package com.zorsecyber.bouncer.webapp.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PasswordDto {
    @NotNull(message = "New password should not be null")
    @NotEmpty(message = "Enter the new password")
    private String newPassword;
    @NotNull(message = "Confirm password should not be null")
    @NotEmpty(message = "Enter the confirm password")
    private String confirmPassword;
    private String token;
}
