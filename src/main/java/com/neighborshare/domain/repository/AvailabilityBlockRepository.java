package com.neighborshare.domain.repository;

import com.neighborshare.domain.entity.AvailabilityBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AvailabilityBlockRepository extends JpaRepository<AvailabilityBlock, UUID> {

    List<AvailabilityBlock> findByItemId(UUID itemId);

    @Query("SELECT ab FROM AvailabilityBlock ab WHERE ab.item.id = :itemId " +
           "AND ab.startDate <= :endDate AND ab.endDate >= :startDate")
    List<AvailabilityBlock> findConflictingBlocks(
        @Param("itemId") UUID itemId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT ab FROM AvailabilityBlock ab WHERE ab.item.id = :itemId " +
           "AND ab.blockType = :blockType " +
           "AND ab.startDate <= :endDate AND ab.endDate >= :startDate")
    List<AvailabilityBlock> findBlocksByTypeAndDateRange(
        @Param("itemId") UUID itemId,
        @Param("blockType") String blockType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
