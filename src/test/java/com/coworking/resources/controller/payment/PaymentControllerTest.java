package com.coworking.resources.controller.payment;

import com.coworking.payment.controller.PaymentController;
import com.coworking.payment.dto.PaymentResponse;
import com.coworking.payment.enums.PaymentStatus;
import com.coworking.payment.service.PaymentService;
import com.coworking.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

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
    @WithMockUser(username = "test@mail.com")
    void shouldCreatePaymentIntent() throws Exception {

        when(paymentService.createPaymentIntent(1L, "test@mail.com"))
                .thenReturn("fake_client_secret");

        mockMvc.perform(
                        post("/payments/intent/1")
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSecret")
                        .value("fake_client_secret"));
    }

    @Test
    @WithMockUser(username = "test@mail.com")
    void shouldReturnPaymentHistory() throws Exception {

        PaymentResponse payment = PaymentResponse.builder()
                .id(1L)
                .roomName("Sala Privada")
                .amount(BigDecimal.valueOf(25))
                .currency("usd")
                .status(PaymentStatus.SUCCEEDED)
                .paymentMethod("Stripe")
                .paidAt(Instant.now())
                .build();

        when(paymentService.getMyPayments("test@mail.com"))
                .thenReturn(List.of(payment));

        mockMvc.perform(get("/payments/my-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomName")
                        .value("Sala Privada"))
                .andExpect(jsonPath("$[0].amount")
                        .value(25))
                .andExpect(jsonPath("$[0].paymentMethod")
                        .value("Stripe"));
    }
}