package com.neighborshare.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neighborshare.domain.entity.Booking;
import com.neighborshare.domain.entity.Dispute;
import com.neighborshare.domain.entity.User;
import com.neighborshare.domain.repository.BookingRepository;
import com.neighborshare.domain.repository.DisputeRepository;
import com.neighborshare.domain.repository.UserRepository;
import com.neighborshare.dto.request.CreateDisputeRequest;
import com.neighborshare.dto.response.DisputeResponse;
import com.neighborshare.exception.ResourceNotFoundException;
import com.neighborshare.exception.UnauthorizedException;
import com.neighborshare.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public DisputeResponse createDispute(UUID userId, UUID apartmentId, CreateDisputeRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
            .orElseThrow(() -> new ResourceNotFoundException("Booking", request.getBookingId().toString()));

        boolean isParticipant = booking.getBorrower().getId().equals(userId) || booking.getOwner().getId().equals(userId);
        if (!isParticipant) {
            throw new UnauthorizedException("You are not allowed to dispute this booking");
        }
        if (!booking.getBorrower().getApartment().getId().equals(apartmentId)) {
            throw new UnauthorizedException("Invalid apartment context");
        }
        if (disputeRepository.findByBookingId(booking.getId()).isPresent()) {
            throw new ValidationException("Dispute already exists for this booking");
        }

        User createdBy = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        Dispute dispute = Dispute.builder()
            .booking(booking)
            .createdBy(createdBy)
            .disputeReason(request.getDisputeReason().trim())
            .description(request.getDescription().trim())
            .evidence(toJsonArray(request.getEvidence()))
            .status("open")
            .build();

        return toResponse(disputeRepository.save(dispute));
    }

    @Transactional(readOnly = true)
    public Page<DisputeResponse> listMyDisputes(UUID userId, Pageable pageable) {
        return disputeRepository.findByCreatedById(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DisputeResponse getDispute(UUID userId, UUID disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
            .orElseThrow(() -> new ResourceNotFoundException("Dispute", disputeId.toString()));

        boolean canView = dispute.getCreatedBy().getId().equals(userId)
            || (dispute.getAssignedAdmin() != null && dispute.getAssignedAdmin().getId().equals(userId));
        if (!canView) {
            throw new UnauthorizedException("You are not allowed to view this dispute");
        }
        return toResponse(dispute);
    }

    private String toJsonArray(List<String> evidence) {
        List<String> safeEvidence = evidence == null ? List.of() : evidence;
        try {
            return objectMapper.writeValueAsString(safeEvidence);
        } catch (JsonProcessingException ex) {
            throw new ValidationException("Invalid evidence payload");
        }
    }

    private DisputeResponse toResponse(Dispute dispute) {
        return DisputeResponse.builder()
            .id(dispute.getId())
            .bookingId(dispute.getBooking().getId())
            .createdById(dispute.getCreatedBy().getId())
            .disputeReason(dispute.getDisputeReason())
            .description(dispute.getDescription())
            .evidence(dispute.getEvidence())
            .status(dispute.getStatus())
            .assignedAdminId(dispute.getAssignedAdmin() != null ? dispute.getAssignedAdmin().getId() : null)
            .resolution(dispute.getResolution())
            .refundAmount(dispute.getRefundAmount())
            .createdAt(dispute.getCreatedAt())
            .updatedAt(dispute.getUpdatedAt())
            .resolvedAt(dispute.getResolvedAt())
            .build();
    }
}
