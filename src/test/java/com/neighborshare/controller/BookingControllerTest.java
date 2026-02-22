package com.neighborshare.controller;

import com.neighborshare.config.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neighborshare.dto.request.CreateBookingRequest;
import com.neighborshare.exception.BookingConflictException;
import com.neighborshare.exception.GlobalExceptionHandler;
import com.neighborshare.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    void createBooking_returns400_whenRequestValidationFails() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID apartmentId = UUID.randomUUID();

        mockMvc.perform(post("/v1/bookings")
                .principal(new UsernamePasswordAuthenticationToken(userId.toString(), "n/a"))
                .requestAttr("apartmentId", apartmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void createBooking_returns409_whenServiceThrowsBookingConflict() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID apartmentId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        CreateBookingRequest request = new CreateBookingRequest(
            itemId,
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(1).plusHours(2)
        );

        when(bookingService.createBooking(eq(userId), eq(apartmentId), any(CreateBookingRequest.class)))
            .thenThrow(new BookingConflictException("Item has conflicting bookings in requested time range"));

        mockMvc.perform(post("/v1/bookings")
                .principal(new UsernamePasswordAuthenticationToken(userId.toString(), "n/a"))
                .requestAttr("apartmentId", apartmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("BOOKING_CONFLICT"));
    }

    @Test
    void createBooking_returns401_whenApartmentContextMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        CreateBookingRequest request = new CreateBookingRequest(
            itemId,
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(1).plusHours(2)
        );

        mockMvc.perform(post("/v1/bookings")
                .principal(new UsernamePasswordAuthenticationToken(userId.toString(), "n/a"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }
}
