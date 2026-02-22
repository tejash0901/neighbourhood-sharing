package com.neighborshare.controller;

import com.neighborshare.dto.request.CreateBookingRequest;
import com.neighborshare.dto.request.ReturnBookingRequest;
import com.neighborshare.dto.response.BookingResponse;
import com.neighborshare.exception.UnauthorizedException;
import com.neighborshare.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
        @Valid @RequestBody CreateBookingRequest request,
        Authentication authentication,
        HttpServletRequest httpRequest
    ) {
        UUID userId = extractUserId(authentication);
        UUID apartmentId = extractApartmentId(httpRequest);
        return ResponseEntity.ok(bookingService.createBooking(userId, apartmentId, request));
    }

    @GetMapping("/me/borrowed")
    public ResponseEntity<Page<BookingResponse>> borrowedBookings(
        Authentication authentication,
        HttpServletRequest httpRequest,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        UUID userId = extractUserId(authentication);
        UUID apartmentId = extractApartmentId(httpRequest);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bookingService.listBorrowedBookings(userId, apartmentId, pageable));
    }

    @GetMapping("/me/lent")
    public ResponseEntity<Page<BookingResponse>> lentBookings(
        Authentication authentication,
        HttpServletRequest httpRequest,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        UUID userId = extractUserId(authentication);
        UUID apartmentId = extractApartmentId(httpRequest);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bookingService.listLentBookings(userId, apartmentId, pageable));
    }

    @GetMapping("/me/borrowed/{bookingId}")
    public ResponseEntity<BookingResponse> getBorrowedBooking(
        Authentication authentication,
        HttpServletRequest httpRequest,
        @PathVariable UUID bookingId
    ) {
        UUID userId = extractUserId(authentication);
        UUID apartmentId = extractApartmentId(httpRequest);
        return ResponseEntity.ok(bookingService.getBorrowerBooking(userId, apartmentId, bookingId));
    }

    @GetMapping("/me/lent/{bookingId}")
    public ResponseEntity<BookingResponse> getLentBooking(
        Authentication authentication,
        HttpServletRequest httpRequest,
        @PathVariable UUID bookingId
    ) {
        UUID userId = extractUserId(authentication);
        UUID apartmentId = extractApartmentId(httpRequest);
        return ResponseEntity.ok(bookingService.getOwnerBooking(userId, apartmentId, bookingId));
    }

    @PatchMapping("/{bookingId}/accept")
    public ResponseEntity<BookingResponse> acceptBooking(
        Authentication authentication,
        HttpServletRequest httpRequest,
        @PathVariable UUID bookingId
    ) {
        UUID userId = extractUserId(authentication);
        UUID apartmentId = extractApartmentId(httpRequest);
        return ResponseEntity.ok(bookingService.acceptBooking(userId, apartmentId, bookingId));
    }

    @PatchMapping("/{bookingId}/reject")
    public ResponseEntity<BookingResponse> rejectBooking(
        Authentication authentication,
        HttpServletRequest httpRequest,
        @PathVariable UUID bookingId
    ) {
        UUID userId = extractUserId(authentication);
        UUID apartmentId = extractApartmentId(httpRequest);
        return ResponseEntity.ok(bookingService.rejectBooking(userId, apartmentId, bookingId));
    }

    @PatchMapping("/{bookingId}/mark-active")
    public ResponseEntity<BookingResponse> markActive(
        Authentication authentication,
        HttpServletRequest httpRequest,
        @PathVariable UUID bookingId
    ) {
        UUID userId = extractUserId(authentication);
        UUID apartmentId = extractApartmentId(httpRequest);
        return ResponseEntity.ok(bookingService.markBookingActive(userId, apartmentId, bookingId));
    }

    @PatchMapping("/{bookingId}/return")
    public ResponseEntity<BookingResponse> returnBooking(
        Authentication authentication,
        HttpServletRequest httpRequest,
        @PathVariable UUID bookingId,
        @RequestBody(required = false) ReturnBookingRequest request
    ) {
        UUID userId = extractUserId(authentication);
        UUID apartmentId = extractApartmentId(httpRequest);
        return ResponseEntity.ok(bookingService.returnBooking(userId, apartmentId, bookingId, request));
    }

    @PatchMapping("/{bookingId}/complete")
    public ResponseEntity<BookingResponse> completeBooking(
        Authentication authentication,
        HttpServletRequest httpRequest,
        @PathVariable UUID bookingId
    ) {
        UUID userId = extractUserId(authentication);
        UUID apartmentId = extractApartmentId(httpRequest);
        return ResponseEntity.ok(bookingService.completeBooking(userId, apartmentId, bookingId));
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
