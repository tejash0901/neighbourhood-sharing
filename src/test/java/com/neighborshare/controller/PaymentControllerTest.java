package com.neighborshare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neighborshare.config.JwtProvider;
import com.neighborshare.dto.request.ConfirmPaymentRequest;
import com.neighborshare.dto.response.ApiMessageResponse;
import com.neighborshare.dto.response.PaymentIntentResponse;
import com.neighborshare.exception.GlobalExceptionHandler;
import com.neighborshare.exception.ValidationException;
import com.neighborshare.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    void createOrder_returns401_whenApartmentContextMissing() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/v1/payments/bookings/{bookingId}/order", bookingId)
                .principal(new UsernamePasswordAuthenticationToken(userId.toString(), "n/a")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void confirmPayment_returns400_whenServiceValidationFails() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID apartmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ConfirmPaymentRequest request = new ConfirmPaymentRequest();
        request.setRazorpayOrderId("order_123");
        request.setRazorpayPaymentId("pay_123");
        request.setRazorpaySignature("bad_signature");

        when(paymentService.confirmBookingPayment(
            eq(userId),
            eq(apartmentId),
            eq(bookingId),
            any(),
            any(),
            any()
        )).thenThrow(new ValidationException("Invalid Razorpay payment signature"));

        mockMvc.perform(post("/v1/payments/bookings/{bookingId}/confirm", bookingId)
                .principal(new UsernamePasswordAuthenticationToken(userId.toString(), "n/a"))
                .requestAttr("apartmentId", apartmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void webhook_returns200_whenProcessed() throws Exception {
        String payload = "{\"event\":\"payment.captured\"}";
        String signature = "sig";
        when(paymentService.handleRazorpayWebhook(payload, signature))
            .thenReturn(ApiMessageResponse.builder().message("Webhook processed: payment.captured").build());

        mockMvc.perform(post("/v1/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", signature)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Webhook processed: payment.captured"));
    }

    @Test
    void createOrder_returns200_whenServiceSucceeds() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID apartmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PaymentIntentResponse response = PaymentIntentResponse.builder()
            .orderId("order_abc")
            .keyId("rzp_test_local")
            .amount(1000L)
            .currency("INR")
            .status("created")
            .bookingStatus("ACCEPTED")
            .build();

        when(paymentService.createBookingOrder(userId, apartmentId, bookingId)).thenReturn(response);

        mockMvc.perform(post("/v1/payments/bookings/{bookingId}/order", bookingId)
                .principal(new UsernamePasswordAuthenticationToken(userId.toString(), "n/a"))
                .requestAttr("apartmentId", apartmentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value("order_abc"))
            .andExpect(jsonPath("$.status").value("created"));
    }
}
