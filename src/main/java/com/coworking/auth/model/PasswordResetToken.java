package com.coworking.auth.model;

import com.coworking.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // @JoinColumn(nullable = false)
    @Column(nullable = false)
    private LocalDateTime expiryDate;

    //metodos
    public static PasswordResetToken create(User user) {
        return new PasswordResetToken(
                null,
                UUID.randomUUID().toString(),
                user,
                LocalDateTime.now().plusHours(1) // token valido de 1 hora
        );
    }

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDateTime.now());
    }

}
