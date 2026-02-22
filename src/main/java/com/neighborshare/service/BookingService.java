package com.neighborshare.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neighborshare.domain.entity.Booking;
import com.neighborshare.domain.entity.Item;
import com.neighborshare.domain.entity.User;
import com.neighborshare.domain.repository.BookingRepository;
import com.neighborshare.domain.repository.ItemRepository;
import com.neighborshare.domain.repository.UserRepository;
import com.neighborshare.domain.valueobject.BookingStatus;
import com.neighborshare.dto.request.CreateBookingRequest;
import com.neighborshare.dto.request.ReturnBookingRequest;
import com.neighborshare.dto.response.BookingResponse;
import com.neighborshare.exception.BookingConflictException;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.platform-fee-percent:10}")
    private BigDecimal platformFeePercent;

    @Transactional
    public BookingResponse createBooking(UUID borrowerId, UUID apartmentId, CreateBookingRequest request) {
        User borrower = getUserInApartment(borrowerId, apartmentId);
        Item item = itemRepository.findByIdAndApartmentIdAndDeletedAtIsNull(request.getItemId(), apartmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Item", request.getItemId().toString()));

        if (item.getOwner().getId().equals(borrowerId)) {
            throw new ValidationException("You cannot book your own item");
        }
        if (!item.isAvailableForBooking()) {
            throw new InvalidStateException("Item is not available for booking");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("endDate must be after startDate");
        }

        long days = ChronoUnit.DAYS.between(request.getStartDate().toLocalDate(), request.getEndDate().toLocalDate()) + 1;
        if (days <= 0) {
            throw new ValidationException("Booking duration must be at least one day");
        }
        if (item.getMaxConsecutiveDays() != null && days > item.getMaxConsecutiveDays()) {
            throw new ValidationException("Booking exceeds maxConsecutiveDays for this item");
        }

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
            item.getId(),
            request.getStartDate(),
            request.getEndDate(),
            LocalDateTime.now().minusHours(2)
        );
        if (!conflicts.isEmpty()) {
            throw new BookingConflictException("Item has conflicting bookings in requested time range");
        }

        BigDecimal basePrice = item.getPricePerDay().multiply(BigDecimal.valueOf(days));
        BigDecimal fee = basePrice
            .multiply(platformFeePercent)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal deposit = item.getDepositAmount() == null ? BigDecimal.ZERO : item.getDepositAmount();
        BigDecimal totalAmount = basePrice.add(fee).add(deposit);

        Booking booking = Booking.builder()
            .item(item)
            .borrower(borrower)
            .owner(item.getOwner())
            .status(BookingStatus.REQUESTED)
            .statusUpdatedAt(LocalDateTime.now())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .basePrice(basePrice)
            .platformFee(fee)
            .depositCollected(deposit)
            .totalAmount(totalAmount)
            .build();

        return toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> listBorrowedBookings(UUID userId, Pageable pageable) {
        return bookingRepository.findByBorrowerId(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> listLentBookings(UUID userId, Pageable pageable) {
        return bookingRepository.findByOwnerId(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBorrowerBooking(UUID userId, UUID bookingId) {
        Booking booking = bookingRepository.findByIdAndBorrowerId(bookingId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId.toString()));
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getOwnerBooking(UUID userId, UUID bookingId) {
        Booking booking = bookingRepository.findByIdAndOwnerId(bookingId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId.toString()));
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse acceptBooking(UUID ownerId, UUID bookingId) {
        Booking booking = bookingRepository.findByIdAndOwnerId(bookingId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId.toString()));

        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw new InvalidStateException("Only requested bookings can be accepted");
        }

        booking.setStatus(BookingStatus.ACCEPTED);
        booking.setStatusUpdatedAt(LocalDateTime.now());
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse rejectBooking(UUID ownerId, UUID bookingId) {
        Booking booking = bookingRepository.findByIdAndOwnerId(bookingId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId.toString()));

        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw new InvalidStateException("Only requested bookings can be rejected");
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setStatusUpdatedAt(LocalDateTime.now());
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse markBookingActive(UUID ownerId, UUID bookingId) {
        Booking booking = bookingRepository.findByIdAndOwnerId(bookingId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId.toString()));

        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new InvalidStateException("Only accepted bookings can be marked active");
        }

        booking.setPaidAt(LocalDateTime.now());
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setStatusUpdatedAt(LocalDateTime.now());
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse returnBooking(UUID borrowerId, UUID bookingId, ReturnBookingRequest request) {
        Booking booking = bookingRepository.findByIdAndBorrowerId(bookingId, borrowerId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId.toString()));

        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new InvalidStateException("Only active bookings can be returned");
        }

        booking.setReturnedAt(LocalDateTime.now());
        booking.setReturnNotes(request != null ? request.getReturnNotes() : null);
        booking.setReturnImages(toJsonArray(request != null ? request.getReturnImages() : null));
        booking.setStatus(BookingStatus.RETURNED);
        booking.setStatusUpdatedAt(LocalDateTime.now());
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse completeBooking(UUID ownerId, UUID bookingId) {
        Booking booking = bookingRepository.findByIdAndOwnerId(bookingId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId.toString()));

        if (booking.getStatus() != BookingStatus.RETURNED && booking.getStatus() != BookingStatus.ACTIVE) {
            throw new InvalidStateException("Only returned/active bookings can be completed");
        }

        if (booking.getReturnedAt() == null) {
            booking.setReturnedAt(LocalDateTime.now());
        }
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setStatusUpdatedAt(LocalDateTime.now());
        return toResponse(bookingRepository.save(booking));
    }

    private User getUserInApartment(UUID userId, UUID apartmentId) {
        return userRepository.findByIdAndApartmentId(userId, apartmentId)
            .orElseThrow(() -> new UnauthorizedException("Invalid user context"));
    }

    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
            .id(booking.getId())
            .itemId(booking.getItem().getId())
            .borrowerId(booking.getBorrower().getId())
            .ownerId(booking.getOwner().getId())
            .status(booking.getStatus())
            .startDate(booking.getStartDate())
            .endDate(booking.getEndDate())
            .durationDays(booking.getDurationDays())
            .basePrice(booking.getBasePrice())
            .depositCollected(booking.getDepositCollected())
            .platformFee(booking.getPlatformFee())
            .totalAmount(booking.getTotalAmount())
            .paidAt(booking.getPaidAt())
            .returnedAt(booking.getReturnedAt())
            .returnNotes(booking.getReturnNotes())
            .returnImages(booking.getReturnImages())
            .createdAt(booking.getCreatedAt())
            .build();
    }

    private String toJsonArray(List<String> images) {
        List<String> safeImages = images == null ? List.of() : images;
        try {
            return objectMapper.writeValueAsString(safeImages);
        } catch (JsonProcessingException ex) {
            throw new ValidationException("Invalid returnImages payload");
        }
    }
}
