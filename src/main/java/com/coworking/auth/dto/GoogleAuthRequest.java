package com.coworking.auth.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleAuthRequest {
    private String accessToken;
    private boolean rememberMe;
}
