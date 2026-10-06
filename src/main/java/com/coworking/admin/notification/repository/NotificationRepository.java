package com.coworking.admin.notification.repository;

import com.coworking.admin.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByAdminIdOrderByCreatedAtDesc(Long adminId);

    long countByAdminIdAndReadFalse(Long adminId);
}