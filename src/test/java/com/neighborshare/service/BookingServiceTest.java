package com.neighborshare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neighborshare.domain.entity.Apartment;
import com.neighborshare.domain.entity.Booking;
import com.neighborshare.domain.entity.Item;
import com.neighborshare.domain.entity.User;
import com.neighborshare.domain.repository.BookingRepository;
import com.neighborshare.domain.repository.ItemRepository;
import com.neighborshare.domain.repository.UserRepository;
import com.neighborshare.domain.valueobject.BookingStatus;
import com.neighborshare.dto.request.CreateBookingRequest;
import com.neighborshare.exception.BookingConflictException;
import com.neighborshare.exception.InvalidStateException;
import com.neighborshare.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BookingService bookingService;

    private UUID apartmentId;
    private UUID ownerId;
    private UUID borrowerId;
    private UUID itemId;
    private UUID bookingId;
    private Apartment apartment;
    private User owner;
    private User borrower;
    private Item item;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingService, "platformFeePercent", BigDecimal.TEN);

        apartmentId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        borrowerId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        bookingId = UUID.randomUUID();

        apartment = Apartment.builder()
            .id(apartmentId)
            .name("Test Apt")
            .inviteCode("DEMO123")
            .address("Addr")
            .city("City")
            .country("IN")
            .createdBy(ownerId)
            .build();

        owner = User.builder()
            .id(ownerId)
            .email("owner@test.com")
            .firstName("Owner")
            .lastName("User")
            .apartment(apartment)
            .build();

        borrower = User.builder()
            .id(borrowerId)
            .email("borrower@test.com")
            .firstName("Borrower")
            .lastName("User")
            .apartment(apartment)
            .build();

        item = Item.builder()
            .id(itemId)
            .owner(owner)
            .apartment(apartment)
            .name("Drill")
            .category("Tools")
            .pricePerHour(BigDecimal.valueOf(10))
            .pricePerDay(BigDecimal.valueOf(100))
            .depositAmount(BigDecimal.valueOf(50))
            .isAvailable(true)
            .maxConsecutiveDays(5)
            .build();
    }

    @Test
    void createBooking_success_setsRequestedAndPersists() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusDays(1);
        CreateBookingRequest request = new CreateBookingRequest(itemId, start, end);

        when(userRepository.findByIdAndApartmentId(borrowerId, apartmentId)).thenReturn(Optional.of(borrower));
        when(itemRepository.findByIdAndApartmentIdAndDeletedAtIsNull(itemId, apartmentId)).thenReturn(Optional.of(item));
        when(bookingRepository.findConflictingBookings(eq(itemId), eq(start), eq(end), any())).thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(bookingId);
            b.setDurationDays(2);
            return b;
        });

        var response = bookingService.createBooking(borrowerId, apartmentId, request);

        assertEquals(bookingId, response.getId());
        assertEquals(BookingStatus.REQUESTED, response.getStatus());
        assertEquals(BigDecimal.valueOf(200), response.getBasePrice());
        assertTrue(BigDecimal.valueOf(20.00).compareTo(response.getPlatformFee()) == 0);
        assertTrue(BigDecimal.valueOf(270.00).compareTo(response.getTotalAmount()) == 0);
    }

    @Test
    void createBooking_rejectsPastStartDate() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(5);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        CreateBookingRequest request = new CreateBookingRequest(itemId, start, end);

        when(userRepository.findByIdAndApartmentId(borrowerId, apartmentId)).thenReturn(Optional.of(borrower));
        when(itemRepository.findByIdAndApartmentIdAndDeletedAtIsNull(itemId, apartmentId)).thenReturn(Optional.of(item));

        assertThrows(ValidationException.class, () -> bookingService.createBooking(borrowerId, apartmentId, request));
    }

    @Test
    void createBooking_rejectsConflictingWindow() {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = start.plusHours(2);
        CreateBookingRequest request = new CreateBookingRequest(itemId, start, end);

        Booking existing = Booking.builder()
            .id(UUID.randomUUID())
            .item(item)
            .borrower(borrower)
            .owner(owner)
            .status(BookingStatus.ACCEPTED)
            .startDate(start.minusHours(1))
            .endDate(end.plusHours(1))
            .basePrice(BigDecimal.TEN)
            .totalAmount(BigDecimal.TEN)
            .build();

        when(userRepository.findByIdAndApartmentId(borrowerId, apartmentId)).thenReturn(Optional.of(borrower));
        when(itemRepository.findByIdAndApartmentIdAndDeletedAtIsNull(itemId, apartmentId)).thenReturn(Optional.of(item));
        when(bookingRepository.findConflictingBookings(eq(itemId), eq(start), eq(end), any())).thenReturn(List.of(existing));

        assertThrows(BookingConflictException.class, () -> bookingService.createBooking(borrowerId, apartmentId, request));
    }

    @Test
    void acceptBooking_rejectsWhenAcceptedOverlapExists() {
        Booking toAccept = Booking.builder()
            .id(bookingId)
            .item(item)
            .owner(owner)
            .borrower(borrower)
            .status(BookingStatus.REQUESTED)
            .startDate(LocalDateTime.now().plusDays(1))
            .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
            .basePrice(BigDecimal.TEN)
            .totalAmount(BigDecimal.TEN)
            .build();

        Booking accepted = Booking.builder()
            .id(UUID.randomUUID())
            .item(item)
            .owner(owner)
            .borrower(borrower)
            .status(BookingStatus.ACCEPTED)
            .startDate(toAccept.getStartDate().minusMinutes(30))
            .endDate(toAccept.getEndDate().plusMinutes(30))
            .basePrice(BigDecimal.TEN)
            .totalAmount(BigDecimal.TEN)
            .build();

        when(bookingRepository.findByIdAndOwnerId(bookingId, ownerId)).thenReturn(Optional.of(toAccept));
        when(bookingRepository.findConflictingBookings(eq(itemId), eq(toAccept.getStartDate()), eq(toAccept.getEndDate()), any()))
            .thenReturn(List.of(accepted));

        assertThrows(BookingConflictException.class, () -> bookingService.acceptBooking(ownerId, apartmentId, bookingId));
    }

    @Test
    void markBookingActive_requiresPaymentFirst() {
        Booking accepted = Booking.builder()
            .id(bookingId)
            .item(item)
            .owner(owner)
            .borrower(borrower)
            .status(BookingStatus.ACCEPTED)
            .startDate(LocalDateTime.now().minusMinutes(1))
            .endDate(LocalDateTime.now().plusHours(2))
            .basePrice(BigDecimal.TEN)
            .totalAmount(BigDecimal.TEN)
            .paidAt(null)
            .build();

        when(bookingRepository.findByIdAndOwnerId(bookingId, ownerId)).thenReturn(Optional.of(accepted));

        assertThrows(InvalidStateException.class, () -> bookingService.markBookingActive(ownerId, apartmentId, bookingId));
    }

    @Test
    void completeBooking_requiresReturnedStatus() {
        Booking active = Booking.builder()
            .id(bookingId)
            .item(item)
            .owner(owner)
            .borrower(borrower)
            .status(BookingStatus.ACTIVE)
            .startDate(LocalDateTime.now().minusDays(1))
            .endDate(LocalDateTime.now().plusDays(1))
            .basePrice(BigDecimal.TEN)
            .totalAmount(BigDecimal.TEN)
            .build();

        when(bookingRepository.findByIdAndOwnerId(bookingId, ownerId)).thenReturn(Optional.of(active));

        assertThrows(InvalidStateException.class, () -> bookingService.completeBooking(ownerId, apartmentId, bookingId));
    }

    @Test
    void completeBooking_fromReturned_movesToCompleted() {
        Booking returned = Booking.builder()
            .id(bookingId)
            .item(item)
            .owner(owner)
            .borrower(borrower)
            .status(BookingStatus.RETURNED)
            .returnedAt(LocalDateTime.now())
            .startDate(LocalDateTime.now().minusDays(1))
            .endDate(LocalDateTime.now().minusHours(1))
            .basePrice(BigDecimal.TEN)
            .totalAmount(BigDecimal.TEN)
            .build();

        when(bookingRepository.findByIdAndOwnerId(bookingId, ownerId)).thenReturn(Optional.of(returned));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = bookingService.completeBooking(ownerId, apartmentId, bookingId);

        assertEquals(BookingStatus.COMPLETED, response.getStatus());
        verify(bookingRepository).save(any(Booking.class));
    }
}
