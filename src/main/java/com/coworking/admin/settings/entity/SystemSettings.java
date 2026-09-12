package com.coworking.admin.settings.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "system_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String configKey;

    @Column(nullable = false)
    private LocalTime openingTime;

    @Column(nullable = false)
    private LocalTime closingTime;

    @Column(nullable = false)
    private Integer maxReservationHours;

    @Column(nullable = false)
    private Integer pendingExpirationMinutes;

    @Column(nullable = false)
    private String institutionName;

    private String institutionEmail;

    private String institutionPhone;

    @Column(columnDefinition = "TEXT")
    private String institutionAddress;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}