package com.neighborshare.domain.repository;

import com.neighborshare.domain.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {

    List<Item> findByOwnerId(UUID ownerId);

    List<Item> findByApartmentId(UUID apartmentId);

    Page<Item> findByApartmentIdAndDeletedAtIsNull(UUID apartmentId, Pageable pageable);

    Page<Item> findByApartmentIdAndCategoryAndDeletedAtIsNull(UUID apartmentId, String category, Pageable pageable);

    List<Item> findByApartmentIdAndIsAvailableTrueAndDeletedAtIsNull(UUID apartmentId);

    List<Item> findByOwnerIdAndDeletedAtIsNull(UUID ownerId);

    Optional<Item> findByIdAndDeletedAtIsNull(UUID id);

    List<String> findDistinctCategoryByApartmentIdAndDeletedAtIsNull(UUID apartmentId);

    default List<Item> findAvailableByApartmentId(UUID apartmentId) {
        return findByApartmentIdAndIsAvailableTrueAndDeletedAtIsNull(apartmentId);
    }

    default List<Item> findNotDeletedByOwnerId(UUID ownerId) {
        return findByOwnerIdAndDeletedAtIsNull(ownerId);
    }

    default List<String> findCategoriesByApartmentId(UUID apartmentId) {
        return findDistinctCategoryByApartmentIdAndDeletedAtIsNull(apartmentId);
    }
}
