package com.neighborshare.domain.repository;

import com.neighborshare.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndApartmentId(String email, UUID apartmentId);

    Optional<User> findByIdAndApartmentId(UUID id, UUID apartmentId);

    List<User> findByApartmentId(UUID apartmentId);

    Page<User> findByApartmentId(UUID apartmentId, Pageable pageable);

    List<User> findByApartmentIdAndIsActiveTrue(UUID apartmentId);

    Optional<User> findByEmailAndIsActiveTrue(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndPhoneAndApartmentId(String email, String phone, UUID apartmentId);

    default List<User> findActiveUsersByApartmentId(UUID apartmentId) {
        return findByApartmentIdAndIsActiveTrue(apartmentId);
    }

    default Optional<User> findActiveByEmail(String email) {
        return findByEmailAndIsActiveTrue(email);
    }
}
