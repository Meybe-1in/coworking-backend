package com.coworking.controller.auditAdmin;

import com.coworking.admin.audit.controller.AdminAuditController;
import com.coworking.admin.audit.dto.AuditLogRequest;
import com.coworking.admin.audit.dto.AuditLogResponse;
import com.coworking.admin.audit.enums.AuditAction;
import com.coworking.admin.audit.service.AuditLogService;
import com.coworking.admin.dto.AdminPageResponse;
import com.coworking.security.JwtAuthenticationFilter;
import com.coworking.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminAuditController.class)
@AutoConfigureMockMvc
class AdminAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {

        doAnswer(invocation -> {
            invocation.<jakarta.servlet.FilterChain>getArgument(2)
                    .doFilter(
                            invocation.getArgument(0),
                            invocation.getArgument(1)
                    );
            return null;
        }).when(jwtAuthenticationFilter)
                .doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnAuditLogsSuccessfully() throws Exception {

        AuditLogResponse auditLog =
                new AuditLogResponse(
                        1L,
                        99L,
                        "admin",
                        AuditAction.ROOM_CREATED,
                        "Room",
                        10L,
                        Instant.parse("2026-09-01T15:00:00Z")
                );

        AdminPageResponse<AuditLogResponse> response =
                AdminPageResponse.<AuditLogResponse>builder()
                        .content(List.of(auditLog))
                        .page(0)
                        .size(10)
                        .totalElements(1L)
                        .totalPages(1)
                        .first(true)
                        .last(true)
                        .build();

        when(auditLogService.getAuditLogs(
                any(AuditLogRequest.class),
                eq(0),
                eq(10)
        )).thenReturn(response);

        mockMvc.perform(
                        get("/admin/audit-logs")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].adminId").value(99))
                .andExpect(jsonPath("$.content[0].adminName").value("admin"))
                .andExpect(jsonPath("$.content[0].action")
                        .value("ROOM_CREATED"))
                .andExpect(jsonPath("$.content[0].entityType")
                        .value("Room"))
                .andExpect(jsonPath("$.content[0].entityId")
                        .value(10))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnAuditLogsWithFilters() throws Exception {

        AdminPageResponse<AuditLogResponse> response =
                AdminPageResponse.<AuditLogResponse>builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0L)
                        .totalPages(0)
                        .first(true)
                        .last(true)
                        .build();

        when(auditLogService.getAuditLogs(
                any(AuditLogRequest.class),
                eq(0),
                eq(10)
        )).thenReturn(response);

        mockMvc.perform(
                        get("/admin/audit-logs")
                                .param("adminName", "admin")
                                .param("startDate", "2026-09-01")
                                .param("endDate", "2026-09-03")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectInvalidDateRange() throws Exception {

        mockMvc.perform(
                        get("/admin/audit-logs")
                                .param("startDate", "2026-09-10")
                                .param("endDate", "2026-09-01")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUseDefaultPagination() throws Exception {

        AdminPageResponse<AuditLogResponse> response =
                AdminPageResponse.<AuditLogResponse>builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0L)
                        .totalPages(0)
                        .first(true)
                        .last(true)
                        .build();

        when(auditLogService.getAuditLogs(
                any(AuditLogRequest.class),
                eq(0),
                eq(10)
        )).thenReturn(response);

        mockMvc.perform(
                        get("/admin/audit-logs")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    //export csv
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldExportAuditLogsCsvSuccessfully() throws Exception {

        byte[] csv = (
                "ID,Administrador,Acción,Entidad,ID Entidad,Fecha\n" +
                        "1,admin,ROOM_CREATED,Room,10,01/09/2026 09:00\n"
        ).getBytes(java.nio.charset.StandardCharsets.UTF_8);

        when(auditLogService.exportAuditLogsCsv(
                any(AuditLogRequest.class)
        )).thenReturn(csv);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .post("/admin/audit-logs/export/csv")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "adminName": "admin",
                                      "startDate": "2026-09-01",
                                      "endDate": "2026-09-03"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"audit-logs.csv\""
                        )
                )
                .andExpect(
                        content().contentType("text/csv")
                )
                .andExpect(
                        content().bytes(csv)
                );

        verify(auditLogService).exportAuditLogsCsv(
                any(AuditLogRequest.class)
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldExportAuditLogsCsvWithoutFilters() throws Exception {

        byte[] csv = (
                "ID,Administrador,Acción,Entidad,ID Entidad,Fecha\n"
        ).getBytes(java.nio.charset.StandardCharsets.UTF_8);

        when(auditLogService.exportAuditLogsCsv(
                any(AuditLogRequest.class)
        )).thenReturn(csv);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .post("/admin/audit-logs/export/csv")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"audit-logs.csv\""
                        )
                )
                .andExpect(
                        content().contentType("text/csv")
                );

        verify(auditLogService).exportAuditLogsCsv(
                any(AuditLogRequest.class)
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectInvalidDateRangeWhenExportingCsv() throws Exception {

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .post("/admin/audit-logs/export/csv")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "startDate": "2026-09-10",
                                      "endDate": "2026-09-01"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest());

        verify(
                auditLogService,
                never()
        ).exportAuditLogsCsv(
                any(AuditLogRequest.class)
        );
    }
}