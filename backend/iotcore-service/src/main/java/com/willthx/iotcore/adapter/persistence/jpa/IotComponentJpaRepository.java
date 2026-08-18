package com.willthx.iotcore.adapter.persistence.jpa;

import com.willthx.iotcore.adapter.persistence.entity.IotComponentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IotComponentJpaRepository extends JpaRepository<IotComponentEntity, Long> {

    List<IotComponentEntity> findAllByMachineId(Long machineId);

    List<IotComponentEntity> findAllByStationIdAndMachineIdIsNull(Long stationId);

    Optional<IotComponentEntity> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByMachineIdAndComponentCode(Long machineId, String componentCode);

    boolean existsByStationIdAndMachineIdIsNullAndComponentCode(Long stationId, String componentCode);

    int countByStationIdAndIsActiveTrue(Long stationId);

    List<IotComponentEntity> findByMachineIdAndIsActiveTrue(Long machineId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE IotComponentEntity c SET c.isActive = false WHERE c.machineId = :machineId AND c.isActive = true")
    int deactivateActiveByMachineId(@Param("machineId") Long machineId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE IotComponentEntity c SET c.isActive = false WHERE c.machineId IN :machineIds AND c.isActive = true")
    int deactivateActiveByMachineIds(@Param("machineIds") List<Long> machineIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE IotComponentEntity c SET c.isActive = false WHERE c.stationId = :stationId AND c.machineId IS NULL AND c.isActive = true")
    int deactivateStationLevelActiveByStationId(@Param("stationId") Long stationId);
}
