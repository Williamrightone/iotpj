package com.willthx.iotcore.adapter.persistence.jpa;

import com.willthx.iotcore.adapter.persistence.entity.StationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StationJpaRepository extends JpaRepository<StationEntity, Long> {

    List<StationEntity> findAllByTenantIdOrderBySortOrder(Long tenantId);

    Optional<StationEntity> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndStationCode(Long tenantId, String stationCode);

    @Query("SELECT COALESCE(MAX(s.sortOrder), 0) FROM StationEntity s WHERE s.tenantId = :tenantId")
    int findMaxSortOrderByTenantId(@Param("tenantId") Long tenantId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE StationEntity s SET s.sortOrder = :sortOrder WHERE s.id = :id AND s.tenantId = :tenantId")
    void updateSortOrder(@Param("id") Long id,
                         @Param("tenantId") Long tenantId,
                         @Param("sortOrder") int sortOrder);
}
