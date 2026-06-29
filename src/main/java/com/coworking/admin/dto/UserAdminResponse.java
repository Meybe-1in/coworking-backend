package com.coworking.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAdminResponse {

    private Long id;

    private String username;

    private String email;

    private Set<String> roles;

    private boolean enabled;

    private boolean emailVerified;

    private LocalDateTime createdAt;
}
