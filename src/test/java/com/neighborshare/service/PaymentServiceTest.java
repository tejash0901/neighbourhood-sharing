package com.neighborshare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neighborshare.domain.entity.Apartment;
import com.neighborshare.domain.entity.Booking;
import com.neighborshare.domain.entity.Item;
import com.neighborshare.domain.entity.Transaction;
import com.neighborshare.domain.entity.User;
import com.neighborshare.domain.repository.BookingRepository;
import com.neighborshare.domain.repository.TransactionRepository;
import com.neighborshare.domain.valueobject.BookingStatus;
import com.neighborshare.exception.InvalidStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private PaymentService paymentService;
    private UUID apartmentId;
    private UUID borrowerId;
    private UUID bookingId;
    private Booking booking;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(bookingRepository, transactionRepository, new ObjectMapper());
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_local");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "test_secret_123");
        ReflectionTestUtils.setField(paymentService, "razorpayWebhookSecret", "test_webhook_123");

        apartmentId = UUID.randomUUID();
        borrowerId = UUID.randomUUID();
        bookingId = UUID.randomUUID();

        Apartment apartment = Apartment.builder()
            .id(apartmentId)
            .name("Apt")
            .inviteCode("DEMO123")
            .address("Addr")
            .city("City")
            .country("IN")
            .createdBy(UUID.randomUUID())
            .build();

        User borrower = User.builder()
            .id(borrowerId)
            .email("borrower@test.com")
            .firstName("Borrower")
            .lastName("User")
            .apartment(apartment)
            .build();

        User owner = User.builder()
            .id(UUID.randomUUID())
            .email("owner@test.com")
            .firstName("Owner")
            .lastName("User")
            .apartment(apartment)
            .build();

        Item item = Item.builder()
            .id(UUID.randomUUID())
            .owner(owner)
            .apartment(apartment)
            .name("Drill")
            .category("Tools")
            .pricePerHour(BigDecimal.TEN)
            .pricePerDay(BigDecimal.valueOf(100))
            .depositAmount(BigDecimal.valueOf(50))
            .isAvailable(true)
            .build();

        booking = Booking.builder()
            .id(bookingId)
            .item(item)
            .borrower(borrower)
            .owner(owner)
            .status(BookingStatus.ACCEPTED)
            .startDate(LocalDateTime.now().minusMinutes(5))
            .endDate(LocalDateTime.now().plusHours(2))
            .totalAmount(BigDecimal.valueOf(220))
            .basePrice(BigDecimal.valueOf(200))
            .platformFee(BigDecimal.valueOf(20))
            .depositCollected(BigDecimal.ZERO)
            .paymentIntentId("order_test_1")
            .build();
    }

    @Test
    void confirmBookingPayment_idempotentWhenAlreadyPaid() {
        booking.setPaidAt(LocalDateTime.now().minusMinutes(1));
        booking.setStatus(BookingStatus.ACTIVE);

        String paymentId = "pay_1";
        String signature = sign("order_test_1|pay_1", "test_secret_123");

        when(bookingRepository.findByIdAndBorrowerId(bookingId, borrowerId)).thenReturn(Optional.of(booking));

        var response = paymentService.confirmBookingPayment(
            borrowerId,
            apartmentId,
            bookingId,
            "order_test_1",
            paymentId,
            signature
        );

        assertEquals("paid", response.getStatus());
        assertEquals("ACTIVE", response.getBookingStatus());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void confirmBookingPayment_rejectsNonAcceptedStatus() {
        booking.setStatus(BookingStatus.REQUESTED);
        booking.setPaidAt(null);
        String paymentId = "pay_2";
        String signature = sign("order_test_1|pay_2", "test_secret_123");

        when(bookingRepository.findByIdAndBorrowerId(bookingId, borrowerId)).thenReturn(Optional.of(booking));

        assertThrows(
            InvalidStateException.class,
            () -> paymentService.confirmBookingPayment(
                borrowerId,
                apartmentId,
                bookingId,
                "order_test_1",
                paymentId,
                signature
            )
        );
    }

    @Test
    void confirmBookingPayment_setsPaidAndActivatesWhenStartReached() {
        String paymentId = "pay_3";
        String signature = sign("order_test_1|pay_3", "test_secret_123");

        when(bookingRepository.findByIdAndBorrowerId(bookingId, borrowerId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findByStripeTransactionId("order_test_1")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.confirmBookingPayment(
            borrowerId,
            apartmentId,
            bookingId,
            "order_test_1",
            paymentId,
            signature
        );

        assertEquals("paid", response.getStatus());
        assertEquals("ACTIVE", response.getBookingStatus());
        assertEquals(BookingStatus.ACTIVE, booking.getStatus());
        assertTrue(booking.getPaidAt() != null);
        verify(bookingRepository).save(any(Booking.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void webhookPaymentCaptured_duplicateIsIdempotent() {
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setPaidAt(LocalDateTime.now().minusMinutes(1));
        String payload = """
            {
              "event": "payment.captured",
              "payload": {
                "payment": {
                  "entity": {
                    "order_id": "order_test_1",
                    "id": "pay_dup"
                  }
                }
              }
            }
            """;
        String signature = sign(payload, "test_webhook_123");

        when(bookingRepository.findByPaymentIntentId("order_test_1")).thenReturn(Optional.of(booking));
        when(transactionRepository.findByStripeTransactionId("order_test_1")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.handleRazorpayWebhook(payload, signature);

        assertEquals("Webhook processed: payment.captured", response.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    private String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
