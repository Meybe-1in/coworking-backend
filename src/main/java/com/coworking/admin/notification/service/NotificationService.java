package com.coworking.admin.notification.service;

import com.coworking.admin.notification.dto.NotificationResponse;
import com.coworking.admin.notification.enums.NotificationType;
import com.coworking.admin.notification.model.Notification;
import com.coworking.admin.notification.repository.NotificationRepository;
import com.coworking.user.model.User;
import com.coworking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String adminEmail) {

        User admin = getAdmin(adminEmail);

        return notificationRepository
                .findByAdminIdOrderByCreatedAtDesc(admin.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void markAsRead(Long notificationId, String adminEmail) {

        User admin = getAdmin(adminEmail);

        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Notificación no encontrada"));

        validateOwnership(notification, admin);

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    public void markAllAsRead(String adminEmail) {

        User admin = getAdmin(adminEmail);

        List<Notification> notifications =
                notificationRepository.findByAdminIdOrderByCreatedAtDesc(admin.getId());

        notifications.stream()
                .filter(notification -> !notification.isRead())
                .forEach(notification -> notification.setRead(true));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String adminEmail) {

        User admin = getAdmin(adminEmail);

        return notificationRepository.countByAdminIdAndReadFalse(admin.getId());
    }

    public void createNotificationForAdmins(
            NotificationType type,
            String title,
            String message,
            String entityType,
            Long entityId
    ) {
        List<User> admins = userRepository.findByRoles_Name("ROLE_ADMIN");

        List<Notification> notifications = admins.stream()
                .map(admin -> {
                    Notification notification = new Notification();
                    notification.setAdmin(admin);
                    notification.setType(type);
                    notification.setTitle(title);
                    notification.setMessage(message);
                    notification.setEntityType(entityType);
                    notification.setEntityId(entityId);
                    notification.setRead(false);
                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);
    }

    private User getAdmin(String adminEmail) {

        return userRepository.findByEmail(adminEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Administrador no encontrado"));
    }

    private void validateOwnership(Notification notification, User admin) {

        if (!notification.getAdmin().getId().equals(admin.getId())) {
            throw new IllegalArgumentException(
                    "La notificación no pertenece al administrador"
            );
        }
    }

    private NotificationResponse toResponse(Notification notification) {

        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getEntityType(),
                notification.getEntityId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}