package com.neighborshare.domain.repository;

import com.neighborshare.domain.entity.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApartmentRepository extends JpaRepository<Apartment, UUID> {

    Optional<Apartment> findByInviteCode(String inviteCode);

    Optional<Apartment> findByName(String name);

    boolean existsByInviteCode(String inviteCode);

    boolean existsByName(String name);
}
