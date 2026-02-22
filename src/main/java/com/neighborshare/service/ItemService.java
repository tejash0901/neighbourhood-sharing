package com.neighborshare.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neighborshare.domain.entity.Item;
import com.neighborshare.domain.entity.User;
import com.neighborshare.domain.repository.ItemRepository;
import com.neighborshare.domain.repository.UserRepository;
import com.neighborshare.dto.request.CreateItemRequest;
import com.neighborshare.dto.request.UpdateItemRequest;
import com.neighborshare.dto.response.ApiMessageResponse;
import com.neighborshare.dto.response.ItemResponse;
import com.neighborshare.dto.response.UserResponse;
import com.neighborshare.exception.ResourceNotFoundException;
import com.neighborshare.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ItemResponse createItem(UUID userId, UUID apartmentId, CreateItemRequest request) {
        User owner = getUserInApartment(userId, apartmentId);

        Item item = Item.builder()
            .owner(owner)
            .apartment(owner.getApartment())
            .name(request.getName().trim())
            .description(request.getDescription())
            .category(request.getCategory().trim())
            .pricePerHour(request.getPricePerHour())
            .pricePerDay(request.getPricePerDay())
            .depositAmount(defaultIfNull(request.getDepositAmount(), BigDecimal.ZERO))
            .isAvailable(defaultIfNull(request.getIsAvailable(), true))
            .maxConsecutiveDays(defaultIfNull(request.getMaxConsecutiveDays(), 7))
            .images(toJsonArray(request.getImages()))
            .currentCondition(defaultIfBlank(request.getCurrentCondition(), "good"))
            .damageNotes(request.getDamageNotes())
            .build();

        return toItemResponse(itemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> listItems(UUID apartmentId, String category, Pageable pageable) {
        Page<Item> page = (category == null || category.isBlank())
            ? itemRepository.findByApartmentIdAndDeletedAtIsNull(apartmentId, pageable)
            : itemRepository.findByApartmentIdAndCategoryAndDeletedAtIsNull(apartmentId, category.trim(), pageable);

        return page.map(this::toItemResponse);
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> listMyItems(UUID userId, Pageable pageable) {
        return itemRepository.findByOwnerIdAndDeletedAtIsNull(userId, pageable).map(this::toItemResponse);
    }

    @Transactional(readOnly = true)
    public ItemResponse getItemById(UUID apartmentId, UUID itemId) {
        Item item = itemRepository.findByIdAndApartmentIdAndDeletedAtIsNull(itemId, apartmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Item", itemId.toString()));
        return toItemResponse(item);
    }

    @Transactional
    public ItemResponse updateItem(UUID userId, UUID apartmentId, UUID itemId, UpdateItemRequest request) {
        Item item = itemRepository.findByIdAndApartmentIdAndDeletedAtIsNull(itemId, apartmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Item", itemId.toString()));

        if (!item.getOwner().getId().equals(userId)) {
            throw new UnauthorizedException("You are not allowed to update this item");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            item.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            item.setCategory(request.getCategory().trim());
        }
        if (request.getPricePerHour() != null) {
            item.setPricePerHour(request.getPricePerHour());
        }
        if (request.getPricePerDay() != null) {
            item.setPricePerDay(request.getPricePerDay());
        }
        if (request.getDepositAmount() != null) {
            item.setDepositAmount(request.getDepositAmount());
        }
        if (request.getIsAvailable() != null) {
            item.setIsAvailable(request.getIsAvailable());
        }
        if (request.getMaxConsecutiveDays() != null) {
            item.setMaxConsecutiveDays(request.getMaxConsecutiveDays());
        }
        if (request.getImages() != null) {
            item.setImages(toJsonArray(request.getImages()));
        }
        if (request.getCurrentCondition() != null && !request.getCurrentCondition().isBlank()) {
            item.setCurrentCondition(request.getCurrentCondition().trim());
        }
        if (request.getDamageNotes() != null) {
            item.setDamageNotes(request.getDamageNotes());
        }

        return toItemResponse(itemRepository.save(item));
    }

    @Transactional
    public ApiMessageResponse deleteItem(UUID userId, UUID apartmentId, UUID itemId) {
        Item item = itemRepository.findByIdAndApartmentIdAndDeletedAtIsNull(itemId, apartmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Item", itemId.toString()));

        if (!item.getOwner().getId().equals(userId)) {
            throw new UnauthorizedException("You are not allowed to delete this item");
        }

        item.setDeletedAt(LocalDateTime.now());
        item.setIsAvailable(false);
        itemRepository.save(item);

        return ApiMessageResponse.builder()
            .message("Item deleted successfully")
            .build();
    }

    @Transactional(readOnly = true)
    public List<String> listCategories(UUID apartmentId) {
        return itemRepository.findCategoriesByApartmentId(apartmentId);
    }

    private User getUserInApartment(UUID userId, UUID apartmentId) {
        return userRepository.findByIdAndApartmentId(userId, apartmentId)
            .orElseThrow(() -> new UnauthorizedException("Invalid user context"));
    }

    private ItemResponse toItemResponse(Item item) {
        User owner = item.getOwner();

        UserResponse ownerResponse = UserResponse.builder()
            .id(owner.getId())
            .email(owner.getEmail())
            .firstName(owner.getFirstName())
            .lastName(owner.getLastName())
            .phone(owner.getPhone())
            .profilePicUrl(owner.getProfilePicUrl())
            .bio(owner.getBio())
            .averageRating(owner.getAverageRating())
            .totalRatings(owner.getTotalRatings())
            .trustScore(owner.getTrustScore())
            .totalBorrowedItems(owner.getTotalBorrowedItems())
            .totalLentItems(owner.getTotalLentItems())
            .createdAt(owner.getCreatedAt())
            .build();

        return ItemResponse.builder()
            .id(item.getId())
            .name(item.getName())
            .description(item.getDescription())
            .category(item.getCategory())
            .pricePerHour(item.getPricePerHour())
            .pricePerDay(item.getPricePerDay())
            .depositAmount(item.getDepositAmount())
            .isAvailable(item.getIsAvailable())
            .maxConsecutiveDays(item.getMaxConsecutiveDays())
            .images(item.getImages())
            .currentCondition(item.getCurrentCondition())
            .averageRating(item.getAverageRating())
            .totalBookings(item.getTotalBookings())
            .owner(ownerResponse)
            .createdAt(item.getCreatedAt())
            .build();
    }

    private String toJsonArray(List<String> images) {
        List<String> safeImages = images == null ? List.of() : images;
        try {
            return objectMapper.writeValueAsString(safeImages);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid images payload", ex);
        }
    }

    private <T> T defaultIfNull(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private String defaultIfBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }
}
