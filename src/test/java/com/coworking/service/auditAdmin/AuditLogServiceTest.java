package com.coworking.service.auditAdmin;

import com.coworking.admin.audit.dto.AuditLogRequest;
import com.coworking.admin.audit.dto.AuditLogResponse;
import com.coworking.admin.audit.enums.AuditAction;
import com.coworking.admin.audit.model.AuditLog;
import com.coworking.admin.audit.repository.AuditLogRepository;
import com.coworking.admin.audit.service.AuditLogServiceImpl;
import com.coworking.admin.dto.AdminPageResponse;
import com.coworking.exception.NotFoundException;
import com.coworking.user.model.User;
import com.coworking.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateAuditLogSuccessfully() {

        User admin = new User();
        admin.setId(99L);
        admin.setEmail("admin@test.com");
        admin.setUsername("admin");

        mockAuthenticatedUser(admin);

        auditLogService.log(
                AuditAction.ROOM_CREATED,
                "Room",
                1L
        );

        verify(userRepository).findByEmail("admin@test.com");

        verify(auditLogRepository).save(
                argThat(auditLog ->
                        auditLog.getAdmin().equals(admin)
                                && auditLog.getAction() == AuditAction.ROOM_CREATED
                                && auditLog.getEntityType().equals("Room")
                                && auditLog.getEntityId().equals(1L)
                )
        );
    }

    @Test
    void shouldThrowWhenAuthenticatedUserDoesNotExist() {

        when(authentication.getName())
                .thenReturn("admin@test.com");

        when(securityContext.getAuthentication())
                .thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> auditLogService.log(
                        AuditAction.ROOM_CREATED,
                        "Room",
                        1L
                )
        );

        verify(auditLogRepository, never())
                .save(any(AuditLog.class));
    }

    @Test
    void shouldReturnAuditLogsSuccessfully() {

        AuditLogRequest request = new AuditLogRequest();

        User admin = new User();
        admin.setId(99L);
        admin.setUsername("admin");
        admin.setEmail("admin@test.com");

        AuditLog auditLog = new AuditLog();
        auditLog.setId(1L);
        auditLog.setAdmin(admin);
        auditLog.setAction(AuditAction.ROOM_CREATED);
        auditLog.setEntityType("Room");
        auditLog.setEntityId(10L);
        auditLog.setCreatedAt(
                Instant.parse("2026-09-01T15:00:00Z")
        );

        Page<AuditLog> page = new PageImpl<>(
                List.of(auditLog)
        );

        when(auditLogRepository.findAll(
                ArgumentMatchers.<Specification<AuditLog>>any(),
                any(Pageable.class)
        )).thenReturn(page);

        AdminPageResponse<AuditLogResponse> response =
                auditLogService.getAuditLogs(
                        request,
                        0,
                        10
                );

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(1L, response.totalElements());
        assertEquals(1L, response.totalPages());

        AuditLogResponse result =
                response.content().get(0);

        assertEquals(1L, result.id());
        assertEquals(99L, result.adminId());
        assertEquals("admin", result.adminName());
        assertEquals(
                AuditAction.ROOM_CREATED,
                result.action()
        );
        assertEquals("Room", result.entityType());
        assertEquals(10L, result.entityId());
        assertEquals(
                Instant.parse("2026-09-01T15:00:00Z"),
                result.createdAt()
        );
    }

    @Test
    void shouldFilterAuditLogsByAdminName() {

        AuditLogRequest request = new AuditLogRequest();
        request.setAdminName("admin");

        Page<AuditLog> page =
                new PageImpl<>(List.of());

        when(auditLogRepository.findAll(
                ArgumentMatchers.<Specification<AuditLog>>any(),
                any(Pageable.class)
        )).thenReturn(page);

        AdminPageResponse<AuditLogResponse> response =
                auditLogService.getAuditLogs(
                        request,
                        0,
                        10
                );

        assertNotNull(response);
        assertTrue(response.content().isEmpty());

        verify(auditLogRepository).findAll(
                ArgumentMatchers.<Specification<AuditLog>>any(),
                any(Pageable.class)
        );
    }

    @Test
    void shouldThrowWhenStartDateIsAfterEndDate() {

        AuditLogRequest request = new AuditLogRequest();

        request.setStartDate(
                LocalDate.of(2026, 9, 10)
        );

        request.setEndDate(
                LocalDate.of(2026, 9, 1)
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> auditLogService.getAuditLogs(
                                request,
                                0,
                                10
                        )
                );

        assertEquals(
                "La fecha de inicio no puede ser posterior a la fecha de fin",
                exception.getMessage()
        );

        verify(auditLogRepository, never()).findAll(
                ArgumentMatchers.<Specification<AuditLog>>any(),
                any(Pageable.class)
        );
    }

    private void mockAuthenticatedUser(User user) {

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(authentication.getName())
                .thenReturn(user.getEmail());

        when(securityContext.getAuthentication())
                .thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    //export csv
    @Test
    void shouldExportAuditLogsCsvSuccessfully() {

        AuditLogRequest request = new AuditLogRequest();

        User admin = new User();
        admin.setId(99L);
        admin.setUsername("admin");
        admin.setEmail("admin@test.com");

        AuditLog auditLog = new AuditLog();
        auditLog.setId(1L);
        auditLog.setAdmin(admin);
        auditLog.setAction(AuditAction.ROOM_CREATED);
        auditLog.setEntityType("Room");
        auditLog.setEntityId(10L);
        auditLog.setCreatedAt(
                Instant.parse("2026-09-01T15:00:00Z")
        );

        when(auditLogRepository.findAll(
                ArgumentMatchers.<Specification<AuditLog>>any(),
                any(Sort.class)
        )).thenReturn(List.of(auditLog));

        byte[] result =
                auditLogService.exportAuditLogsCsv(request);

        assertNotNull(result);

        String csv = new String(
                result,
                StandardCharsets.UTF_8
        );

        assertTrue(csv.contains(
                "ID,Administrador,Acción,Entidad,ID Entidad,Fecha"
        ));

        assertTrue(csv.contains("admin"));
        assertTrue(csv.contains("ROOM_CREATED"));
        assertTrue(csv.contains("Room"));
        assertTrue(csv.contains("10"));

        // 15:00 UTC = 09:00 en El Salvador
        assertTrue(csv.contains("01/09/2026 09:00"));

        verify(auditLogRepository).findAll(
                ArgumentMatchers.<Specification<AuditLog>>any(),
                any(Sort.class)
        );
    }

    @Test
    void shouldExportAuditLogsFilteredByAdminName() {

        AuditLogRequest request = new AuditLogRequest();
        request.setAdminName("admin");

        when(auditLogRepository.findAll(
                ArgumentMatchers.<Specification<AuditLog>>any(),
                any(Sort.class)
        )).thenReturn(List.of());

        byte[] result =
                auditLogService.exportAuditLogsCsv(request);

        assertNotNull(result);

        String csv = new String(
                result,
                StandardCharsets.UTF_8
        );

        assertTrue(csv.contains(
                "ID,Administrador,Acción,Entidad,ID Entidad,Fecha"
        ));

        verify(auditLogRepository).findAll(
                ArgumentMatchers.<Specification<AuditLog>>any(),
                any(Sort.class)
        );
    }

    @Test
    void shouldExportAuditLogsFilteredByDateRange() {

        AuditLogRequest request = new AuditLogRequest();

        request.setStartDate(
                LocalDate.of(2026, 9, 1)
        );

        request.setEndDate(
                LocalDate.of(2026, 9, 3)
        );

        when(auditLogRepository.findAll(
                ArgumentMatchers.<Specification<AuditLog>>any(),
                any(Sort.class)
        )).thenReturn(List.of());

        byte[] result =
                auditLogService.exportAuditLogsCsv(request);

        assertNotNull(result);

        verify(auditLogRepository).findAll(
                ArgumentMatchers.<Specification<AuditLog>>any(),
                any(Sort.class)
        );
    }

    @Test
    void shouldThrowWhenExportingWithInvalidDateRange() {

        AuditLogRequest request = new AuditLogRequest();

        request.setStartDate(
                LocalDate.of(2026, 9, 10)
        );

        request.setEndDate(
                LocalDate.of(2026, 9, 1)
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> auditLogService.exportAuditLogsCsv(request)
                );

        assertEquals(
                "La fecha de inicio no puede ser posterior a la fecha de fin",
                exception.getMessage()
        );

        verify(auditLogRepository, never()).findAll(
                ArgumentMatchers.<Specification<AuditLog>>any(),
                any(Sort.class)
        );
    }

    @Test
    void shouldExportEmptyAuditLogsCsv() {

        AuditLogRequest request = new AuditLogRequest();

        when(auditLogRepository.findAll(
                ArgumentMatchers.<Specification<AuditLog>>any(),
                any(Sort.class)
        )).thenReturn(List.of());

        byte[] result =
                auditLogService.exportAuditLogsCsv(request);

        String csv = new String(
                result,
                StandardCharsets.UTF_8
        );

        assertTrue(csv.contains(
                "ID,Administrador,Acción,Entidad,ID Entidad,Fecha"
        ));

        verify(auditLogRepository).findAll(
                ArgumentMatchers.<Specification<AuditLog>>any(),
                any(Sort.class)
        );
    }
}