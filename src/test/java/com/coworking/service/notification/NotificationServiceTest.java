package com.coworking.service.notification;

import com.coworking.admin.notification.dto.NotificationResponse;
import com.coworking.admin.notification.enums.NotificationType;
import com.coworking.admin.notification.model.Notification;
import com.coworking.admin.notification.repository.NotificationRepository;
import com.coworking.admin.notification.service.NotificationService;
import com.coworking.user.model.User;
import com.coworking.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void shouldGetNotificationsSuccessfully() {

        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@test.com");

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setAdmin(admin);
        notification.setType(NotificationType.RESERVATION_EXPIRED);
        notification.setTitle("Reserva expirada");
        notification.setMessage("La reserva #1 ha expirado.");
        notification.setEntityType("Reservation");
        notification.setEntityId(1L);
        notification.setRead(false);

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(admin));

        when(notificationRepository.findByAdminIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(notification));

        List<NotificationResponse> result =
                notificationService.getNotifications("admin@test.com");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals(
                NotificationType.RESERVATION_EXPIRED,
                result.get(0).type()
        );
        assertEquals("Reserva expirada", result.get(0).title());
        assertFalse(result.get(0).read());

        verify(userRepository).findByEmail("admin@test.com");
        verify(notificationRepository)
                .findByAdminIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void shouldMarkNotificationAsReadSuccessfully() {

        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@test.com");

        Notification notification = new Notification();
        notification.setId(10L);
        notification.setAdmin(admin);
        notification.setRead(false);

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(admin));

        when(notificationRepository.findById(10L))
                .thenReturn(Optional.of(notification));

        notificationService.markAsRead(10L, "admin@test.com");

        assertTrue(notification.isRead());

        verify(notificationRepository).save(notification);
    }

    @Test
    void shouldNotSaveNotificationWhenAlreadyRead() {

        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@test.com");

        Notification notification = new Notification();
        notification.setId(10L);
        notification.setAdmin(admin);
        notification.setRead(true);

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(admin));

        when(notificationRepository.findById(10L))
                .thenReturn(Optional.of(notification));

        notificationService.markAsRead(10L, "admin@test.com");

        assertTrue(notification.isRead());

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void shouldMarkAllNotificationsAsReadSuccessfully() {

        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@test.com");

        Notification unreadNotification = new Notification();
        unreadNotification.setId(1L);
        unreadNotification.setAdmin(admin);
        unreadNotification.setRead(false);

        Notification readNotification = new Notification();
        readNotification.setId(2L);
        readNotification.setAdmin(admin);
        readNotification.setRead(true);

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(admin));

        when(notificationRepository.findByAdminIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(unreadNotification, readNotification));

        notificationService.markAllAsRead("admin@test.com");

        assertTrue(unreadNotification.isRead());
        assertTrue(readNotification.isRead());

        verify(notificationRepository, never())
                .save(any(Notification.class));
    }

    @Test
    void shouldGetUnreadCountSuccessfully() {

        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@test.com");

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(admin));

        when(notificationRepository.countByAdminIdAndReadFalse(1L))
                .thenReturn(4L);

        long result =
                notificationService.getUnreadCount("admin@test.com");

        assertEquals(4L, result);

        verify(notificationRepository)
                .countByAdminIdAndReadFalse(1L);
    }

    @Test
    void shouldCreateNotificationForAllAdmins() {

        User adminOne = new User();
        adminOne.setId(1L);
        adminOne.setEmail("admin1@test.com");

        User adminTwo = new User();
        adminTwo.setId(2L);
        adminTwo.setEmail("admin2@test.com");

        when(userRepository.findByRoles_Name("ROLE_ADMIN"))
                .thenReturn(List.of(adminOne, adminTwo));

        notificationService.createNotificationForAdmins(
                NotificationType.USER_BLOCKED,
                "Usuario bloqueado",
                "El usuario dayana ha sido bloqueado.",
                "User",
                5L
        );

        verify(notificationRepository).saveAll(argThat(notifications ->
                ((Collection<Notification>) notifications).size() == 2
                        && ((List<Notification>) notifications).get(0).getAdmin().equals(adminOne)
                        && ((List<Notification>) notifications).get(1).getAdmin().equals(adminTwo)
                        && ((List<Notification>) notifications).get(0).getType()
                        == NotificationType.USER_BLOCKED
                        && ((List<Notification>) notifications).get(1).getType()
                        == NotificationType.USER_BLOCKED
                        && !((List<Notification>) notifications).get(0).isRead()
                        && !((List<Notification>) notifications).get(1).isRead()
        ));
    }

    @Test
    void shouldNotCreateNotificationsWhenThereAreNoAdmins() {

        when(userRepository.findByRoles_Name("ROLE_ADMIN"))
                .thenReturn(List.of());

        notificationService.createNotificationForAdmins(
                NotificationType.ROOM_DELETED,
                "Sala eliminada",
                "La sala #1 ha sido eliminada.",
                "Room",
                1L
        );

        verify(notificationRepository)
                .saveAll(argThat(list -> ((Collection<?>) list).isEmpty()));
    }

    @Test
    void shouldThrowExceptionWhenNotificationDoesNotExist() {

        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@test.com");

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(admin));

        when(notificationRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.markAsRead(
                        10L,
                        "admin@test.com"
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenNotificationDoesNotBelongToAdmin() {

        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@test.com");

        User otherAdmin = new User();
        otherAdmin.setId(2L);
        otherAdmin.setEmail("other@test.com");

        Notification notification = new Notification();
        notification.setId(10L);
        notification.setAdmin(otherAdmin);
        notification.setRead(false);

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(admin));

        when(notificationRepository.findById(10L))
                .thenReturn(Optional.of(notification));

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.markAsRead(
                        10L,
                        "admin@test.com"
                )
        );

        verify(notificationRepository, never())
                .save(any(Notification.class));
    }

    @Test
    void shouldThrowExceptionWhenAdminDoesNotExist() {

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.getNotifications(
                        "admin@test.com"
                )
        );

        verify(notificationRepository, never())
                .findByAdminIdOrderByCreatedAtDesc(anyLong());
    }
}
