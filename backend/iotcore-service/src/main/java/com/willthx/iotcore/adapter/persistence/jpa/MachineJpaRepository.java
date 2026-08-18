package com.willthx.iotcore.adapter.persistence.jpa;

import com.willthx.iotcore.adapter.persistence.entity.MachineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MachineJpaRepository extends JpaRepository<MachineEntity, Long> {

    List<MachineEntity> findAllByStationIdAndTenantId(Long stationId, Long tenantId);

    Optional<MachineEntity> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndMachineCode(Long tenantId, String machineCode);

    List<MachineEntity> findByStationIdAndIsActiveTrue(Long stationId);

    int countByStationIdAndIsActiveTrue(Long stationId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MachineEntity m SET m.isActive = false WHERE m.stationId = :stationId AND m.isActive = true")
    int deactivateActiveByStationId(@Param("stationId") Long stationId);
}
