package com.neighborshare.controller;

import com.neighborshare.dto.request.ConfirmPaymentRequest;
import com.neighborshare.dto.response.ApiMessageResponse;
import com.neighborshare.dto.response.PaymentIntentResponse;
import com.neighborshare.dto.response.TransactionResponse;
import com.neighborshare.exception.UnauthorizedException;
import com.neighborshare.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/bookings/{bookingId}/order")
    public ResponseEntity<PaymentIntentResponse> createBookingPaymentIntent(
        @PathVariable UUID bookingId,
        Authentication authentication,
        HttpServletRequest httpRequest
    ) {
        UUID userId = extractUserId(authentication);
        UUID apartmentId = extractApartmentId(httpRequest);
        return ResponseEntity.ok(paymentService.createBookingOrder(userId, apartmentId, bookingId));
    }

    // Backward-compatible alias for older clients.
    @PostMapping("/bookings/{bookingId}/intent")
    public ResponseEntity<PaymentIntentResponse> createBookingPaymentIntentAlias(
        @PathVariable UUID bookingId,
        Authentication authentication,
        HttpServletRequest httpRequest
    ) {
        UUID userId = extractUserId(authentication);
        UUID apartmentId = extractApartmentId(httpRequest);
        return ResponseEntity.ok(paymentService.createBookingOrder(userId, apartmentId, bookingId));
    }

    @PostMapping("/bookings/{bookingId}/confirm")
    public ResponseEntity<PaymentIntentResponse> confirmBookingPayment(
        @PathVariable UUID bookingId,
        Authentication authentication,
        HttpServletRequest httpRequest,
        @RequestBody(required = false) ConfirmPaymentRequest request
    ) {
        UUID userId = extractUserId(authentication);
        UUID apartmentId = extractApartmentId(httpRequest);
        String orderId = request != null ? request.getRazorpayOrderId() : null;
        String paymentId = request != null ? request.getRazorpayPaymentId() : null;
        String signature = request != null ? request.getRazorpaySignature() : null;
        return ResponseEntity.ok(paymentService.confirmBookingPayment(userId, apartmentId, bookingId, orderId, paymentId, signature));
    }

    @GetMapping("/me/transactions")
    public ResponseEntity<Page<TransactionResponse>> listMyTransactions(
        Authentication authentication,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        UUID userId = extractUserId(authentication);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(paymentService.listMyTransactions(userId, status, pageable));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiMessageResponse> razorpayWebhook(
        @RequestBody String payload,
        @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) {
        return ResponseEntity.ok(paymentService.handleRazorpayWebhook(payload, signature));
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthorizedException("Missing authentication context");
        }
        try {
            return UUID.fromString(authentication.getPrincipal().toString());
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid authentication principal");
        }
    }

    private UUID extractApartmentId(HttpServletRequest request) {
        Object apartmentId = request.getAttribute("apartmentId");
        if (!(apartmentId instanceof UUID)) {
            throw new UnauthorizedException("Missing apartment context");
        }
        return (UUID) apartmentId;
    }
}
