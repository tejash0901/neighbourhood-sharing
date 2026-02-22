package com.neighborshare.controller;

import com.neighborshare.dto.request.CreateDisputeRequest;
import com.neighborshare.dto.response.DisputeResponse;
import com.neighborshare.exception.UnauthorizedException;
import com.neighborshare.service.DisputeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping
    public ResponseEntity<DisputeResponse> createDispute(
        @Valid @RequestBody CreateDisputeRequest request,
        Authentication authentication,
        HttpServletRequest httpRequest
    ) {
        UUID userId = extractUserId(authentication);
        UUID apartmentId = extractApartmentId(httpRequest);
        return ResponseEntity.ok(disputeService.createDispute(userId, apartmentId, request));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<DisputeResponse>> myDisputes(
        Authentication authentication,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(disputeService.listMyDisputes(extractUserId(authentication), pageable));
    }

    @GetMapping("/{disputeId}")
    public ResponseEntity<DisputeResponse> getDispute(
        Authentication authentication,
        @PathVariable UUID disputeId
    ) {
        return ResponseEntity.ok(disputeService.getDispute(extractUserId(authentication), disputeId));
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
