package com.neighborshare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neighborshare.domain.entity.Booking;
import com.neighborshare.domain.entity.Transaction;
import com.neighborshare.domain.repository.BookingRepository;
import com.neighborshare.domain.repository.TransactionRepository;
import com.neighborshare.domain.valueobject.BookingStatus;
import com.neighborshare.dto.response.ApiMessageResponse;
import com.neighborshare.dto.response.PaymentIntentResponse;
import com.neighborshare.dto.response.TransactionResponse;
import com.neighborshare.exception.InvalidStateException;
import com.neighborshare.exception.ResourceNotFoundException;
import com.neighborshare.exception.UnauthorizedException;
import com.neighborshare.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook-secret:}")
    private String razorpayWebhookSecret;

    @Value("${razorpay.base-url:https://api.razorpay.com}")
    private String razorpayBaseUrl;

    @Transactional
    public PaymentIntentResponse createBookingOrder(UUID userId, UUID apartmentId, UUID bookingId) {
        Booking booking = bookingRepository.findByIdAndBorrowerId(bookingId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId.toString()));
        assertBookingInApartment(booking, apartmentId);

        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new InvalidStateException("Order can only be created for accepted bookings");
        }
        if (booking.getPaidAt() != null) {
            throw new InvalidStateException("Booking is already paid");
        }

        ensureRazorpayConfigured();

        try {
            if (booking.getPaymentIntentId() != null && !booking.getPaymentIntentId().isBlank()) {
                upsertPendingTransaction(booking, booking.getPaymentIntentId(), "created");
                return PaymentIntentResponse.builder()
                    .orderId(booking.getPaymentIntentId())
                    .keyId(razorpayKeyId)
                    .amount(toPaise(booking.getTotalAmount()))
                    .currency("INR")
                    .status("created")
                    .bookingStatus(booking.getStatus().name())
                    .build();
            }

            long amountPaise = toPaise(booking.getTotalAmount());
            String body = objectMapper.createObjectNode()
                .put("amount", amountPaise)
                .put("currency", "INR")
                .put("receipt", "booking_" + booking.getId())
                .put("payment_capture", 1)
                .toString();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(razorpayBaseUrl + "/v1/orders"))
                .header("Authorization", basicAuthHeader())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ValidationException("Razorpay order creation failed: " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            String orderId = json.path("id").asText(null);
            if (orderId == null || orderId.isBlank()) {
                throw new ValidationException("Razorpay returned empty order id");
            }

            booking.setPaymentIntentId(orderId);
            bookingRepository.save(booking);
            upsertPendingTransaction(booking, orderId, json.path("status").asText("created"));

            return PaymentIntentResponse.builder()
                .orderId(orderId)
                .keyId(razorpayKeyId)
                .amount(json.path("amount").asLong(amountPaise))
                .currency(json.path("currency").asText("INR"))
                .status(json.path("status").asText("created"))
                .bookingStatus(booking.getStatus().name())
                .build();
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("Razorpay integration error: " + ex.getMessage());
        }
    }

    @Transactional
    public PaymentIntentResponse confirmBookingPayment(
        UUID userId,
        UUID apartmentId,
        UUID bookingId,
        String providedOrderId,
        String paymentId,
        String signature
    ) {
        Booking booking = bookingRepository.findByIdAndBorrowerId(bookingId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId.toString()));
        assertBookingInApartment(booking, apartmentId);

        String orderId = providedOrderId;
        if (orderId == null || orderId.isBlank()) {
            orderId = booking.getPaymentIntentId();
        }
        if (orderId == null || orderId.isBlank()) {
            throw new ValidationException("No Razorpay order found for this booking");
        }
        if (paymentId == null || paymentId.isBlank() || signature == null || signature.isBlank()) {
            throw new ValidationException("razorpayPaymentId and razorpaySignature are required");
        }
        if (!orderId.equals(booking.getPaymentIntentId())) {
            throw new ValidationException("Order id mismatch for booking");
        }

        // Idempotent confirmation: if already paid for this order, return success response.
        if (booking.getPaidAt() != null && (booking.getStatus() == BookingStatus.ACCEPTED || booking.getStatus() == BookingStatus.ACTIVE)) {
            return PaymentIntentResponse.builder()
                .orderId(orderId)
                .keyId(razorpayKeyId)
                .amount(toPaise(booking.getTotalAmount()))
                .currency("INR")
                .status("paid")
                .bookingStatus(booking.getStatus().name())
                .build();
        }
        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new InvalidStateException("Payment can only be confirmed for accepted bookings");
        }
        verifyRazorpayPaymentSignature(orderId, paymentId, signature);

        if (booking.getPaidAt() == null) {
            booking.setPaidAt(LocalDateTime.now());
        }
        if (booking.getStatus() == BookingStatus.ACCEPTED && !LocalDateTime.now().isBefore(booking.getStartDate())) {
            booking.setStatus(BookingStatus.ACTIVE);
            booking.setStatusUpdatedAt(LocalDateTime.now());
        }
        bookingRepository.save(booking);

        upsertCompletedTransaction(booking, orderId, paymentId);

        return PaymentIntentResponse.builder()
            .orderId(orderId)
            .keyId(razorpayKeyId)
            .amount(toPaise(booking.getTotalAmount()))
            .currency("INR")
            .status("paid")
            .bookingStatus(booking.getStatus().name())
            .build();
    }

    @Transactional
    public ApiMessageResponse handleRazorpayWebhook(String payload, String signatureHeader) {
        if (razorpayWebhookSecret == null || razorpayWebhookSecret.isBlank()) {
            return ApiMessageResponse.builder()
                .message("Razorpay webhook secret not configured. Event ignored.")
                .build();
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new ValidationException("Missing webhook signature");
        }

        verifyWebhookSignature(payload, signatureHeader);

        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText("");
            JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
            String orderId = paymentEntity.path("order_id").asText(null);
            String paymentId = paymentEntity.path("id").asText(null);

            if (orderId == null || orderId.isBlank()) {
                return ApiMessageResponse.builder().message("Webhook ignored: missing order_id").build();
            }

            Optional<Booking> bookingOptional = bookingRepository.findByPaymentIntentId(orderId);
            if (bookingOptional.isEmpty()) {
                return ApiMessageResponse.builder().message("Webhook ignored: booking not found").build();
            }

            Booking booking = bookingOptional.get();
            if ("payment.captured".equals(event)) {
                // Idempotent handling for duplicate capture webhooks.
                if (booking.getPaidAt() == null) {
                    booking.setPaidAt(LocalDateTime.now());
                    if (booking.getStatus() == BookingStatus.ACCEPTED && !LocalDateTime.now().isBefore(booking.getStartDate())) {
                        booking.setStatus(BookingStatus.ACTIVE);
                        booking.setStatusUpdatedAt(LocalDateTime.now());
                    }
                    bookingRepository.save(booking);
                }
                upsertCompletedTransaction(booking, orderId, paymentId);
            } else if ("payment.failed".equals(event)) {
                upsertFailedTransaction(booking, orderId, paymentId);
            } else {
                return ApiMessageResponse.builder().message("Webhook ignored: " + event).build();
            }

            return ApiMessageResponse.builder().message("Webhook processed: " + event).build();
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("Invalid webhook payload");
        }
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listMyTransactions(UUID userId, String status, Pageable pageable) {
        Page<Transaction> page = (status == null || status.isBlank())
            ? transactionRepository.findByUserId(userId, pageable)
            : transactionRepository.findByUserIdAndStatus(userId, status.trim().toLowerCase(), pageable);
        return page.map(this::toResponse);
    }

    private void upsertPendingTransaction(Booking booking, String orderId, String orderStatus) {
        Transaction transaction = transactionRepository.findByStripeTransactionId(orderId)
            .orElseGet(() -> Transaction.builder()
                .user(booking.getBorrower())
                .booking(booking)
                .transactionType("booking_payment")
                .amount(booking.getTotalAmount())
                .currency("INR")
                .build());
        transaction.setStripeTransactionId(orderId);
        transaction.setStatus("pending");
        transaction.setDescription("Razorpay order created for booking " + booking.getId());
        transaction.setMetadata("{\"gateway\":\"razorpay\",\"orderStatus\":\"" + orderStatus + "\"}");
        transactionRepository.save(transaction);
    }

    private void upsertCompletedTransaction(Booking booking, String orderId, String paymentId) {
        Transaction transaction = transactionRepository.findByStripeTransactionId(orderId)
            .orElseGet(() -> Transaction.builder()
                .user(booking.getBorrower())
                .booking(booking)
                .transactionType("booking_payment")
                .amount(booking.getTotalAmount())
                .currency("INR")
                .stripeTransactionId(orderId)
                .build());
        transaction.setStatus("completed");
        transaction.setDescription("Razorpay payment captured for booking " + booking.getId());
        transaction.setMetadata("{\"gateway\":\"razorpay\",\"orderId\":\"" + orderId + "\",\"paymentId\":\"" + paymentId + "\"}");
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
    }

    private void upsertFailedTransaction(Booking booking, String orderId, String paymentId) {
        Transaction transaction = transactionRepository.findByStripeTransactionId(orderId)
            .orElseGet(() -> Transaction.builder()
                .user(booking.getBorrower())
                .booking(booking)
                .transactionType("booking_payment")
                .amount(booking.getTotalAmount())
                .currency("INR")
                .stripeTransactionId(orderId)
                .build());
        transaction.setStatus("failed");
        transaction.setDescription("Razorpay payment failed for booking " + booking.getId());
        transaction.setMetadata("{\"gateway\":\"razorpay\",\"orderId\":\"" + orderId + "\",\"paymentId\":\"" + paymentId + "\"}");
        transactionRepository.save(transaction);
    }

    private void verifyRazorpayPaymentSignature(String orderId, String paymentId, String signature) {
        ensureRazorpayConfigured();
        String payload = orderId + "|" + paymentId;
        String expected = hmacSha256Hex(payload, razorpayKeySecret);
        if (!expected.equals(signature)) {
            throw new ValidationException("Invalid Razorpay payment signature");
        }
    }

    private void verifyWebhookSignature(String payload, String signature) {
        String expected = hmacSha256Hex(payload, razorpayWebhookSecret);
        if (!expected.equals(signature)) {
            throw new ValidationException("Invalid Razorpay webhook signature");
        }
    }

    private String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new ValidationException("Unable to compute signature");
        }
    }

    private String basicAuthHeader() {
        String token = razorpayKeyId + ":" + razorpayKeySecret;
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    private long toPaise(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private void ensureRazorpayConfigured() {
        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new ValidationException("Razorpay credentials are not configured");
        }
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
            .id(transaction.getId())
            .bookingId(transaction.getBooking() != null ? transaction.getBooking().getId() : null)
            .transactionType(transaction.getTransactionType())
            .amount(transaction.getAmount())
            .currency(transaction.getCurrency())
            .stripeTransactionId(transaction.getStripeTransactionId())
            .status(transaction.getStatus())
            .description(transaction.getDescription())
            .metadata(transaction.getMetadata())
            .createdAt(transaction.getCreatedAt())
            .completedAt(transaction.getCompletedAt())
            .build();
    }

    private void assertBookingInApartment(Booking booking, UUID apartmentId) {
        UUID bookingApartmentId = booking.getItem().getApartment().getId();
        if (!bookingApartmentId.equals(apartmentId)) {
            throw new UnauthorizedException("Invalid apartment context");
        }
    }
}
