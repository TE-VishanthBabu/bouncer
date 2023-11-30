package com.zorsecyber.bouncer.webapp.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoResponse {
    private String userPrincipalName;
    private String id;
    private String displayName;
    private String surname;
    private String givenName;
}
