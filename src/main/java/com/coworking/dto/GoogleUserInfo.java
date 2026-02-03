package com.coworking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GoogleUserInfo {
    private String sub;
    private String email;
    private String name;
    private Boolean email_verified;
}
