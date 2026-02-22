package com.neighborshare.controller;

import com.neighborshare.dto.request.CreateItemRequest;
import com.neighborshare.dto.request.UpdateItemRequest;
import com.neighborshare.dto.response.ApiMessageResponse;
import com.neighborshare.dto.response.ItemResponse;
import com.neighborshare.exception.UnauthorizedException;
import com.neighborshare.service.ItemService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public ResponseEntity<ItemResponse> createItem(
        @Valid @RequestBody CreateItemRequest request,
        Authentication authentication,
        HttpServletRequest httpRequest
    ) {
        UUID userId = extractUserId(authentication);
        UUID apartmentId = extractApartmentId(httpRequest);
        return ResponseEntity.ok(itemService.createItem(userId, apartmentId, request));
    }

    @GetMapping
    public ResponseEntity<Page<ItemResponse>> listItems(
        @RequestParam(required = false) String category,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        HttpServletRequest httpRequest
    ) {
        UUID apartmentId = extractApartmentId(httpRequest);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(itemService.listItems(apartmentId, category, pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<ItemResponse>> listMyItems(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(itemService.listMyItems(userId, pageable));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> listCategories(HttpServletRequest httpRequest) {
        UUID apartmentId = extractApartmentId(httpRequest);
        return ResponseEntity.ok(itemService.listCategories(apartmentId));
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ItemResponse> getItem(
        @PathVariable UUID itemId,
        HttpServletRequest httpRequest
    ) {
        UUID apartmentId = extractApartmentId(httpRequest);
        return ResponseEntity.ok(itemService.getItemById(apartmentId, itemId));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ItemResponse> updateItem(
        @PathVariable UUID itemId,
        @Valid @RequestBody UpdateItemRequest request,
        Authentication authentication,
        HttpServletRequest httpRequest
    ) {
        UUID userId = extractUserId(authentication);
        UUID apartmentId = extractApartmentId(httpRequest);
        return ResponseEntity.ok(itemService.updateItem(userId, apartmentId, itemId, request));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiMessageResponse> deleteItem(
        @PathVariable UUID itemId,
        Authentication authentication,
        HttpServletRequest httpRequest
    ) {
        UUID userId = extractUserId(authentication);
        UUID apartmentId = extractApartmentId(httpRequest);
        return ResponseEntity.ok(itemService.deleteItem(userId, apartmentId, itemId));
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
