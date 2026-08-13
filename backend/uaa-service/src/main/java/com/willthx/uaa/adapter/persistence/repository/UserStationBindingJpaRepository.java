package com.willthx.uaa.adapter.persistence.repository;

import com.willthx.uaa.adapter.persistence.entity.UserStationBindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserStationBindingJpaRepository extends JpaRepository<UserStationBindingEntity, Long> {

    List<UserStationBindingEntity> findByUserIdAndTenantId(Long userId, Long tenantId);

    void deleteByUserIdAndTenantId(Long userId, Long tenantId);
}
