package com.willthx.uaa.domain.port;

import com.willthx.uaa.domain.model.FeatureModel;

import java.util.List;
import java.util.Optional;

/**
 * 功能儲存庫輸出埠（Output Port）。
 */
public interface FeatureRepository {

    List<FeatureModel> findAllByTenantId(Long tenantId);

    List<FeatureModel> findActiveByTenantId(Long tenantId);

    Optional<FeatureModel> findByIdAndTenantId(Long id, Long tenantId);

    /** 依多個 ID 查詢（用於驗證 role permissions 的 featureIds） */
    List<FeatureModel> findByIdsAndTenantId(List<Long> ids, Long tenantId);

    boolean existsByCodeAndTenantId(String featureCode, Long tenantId);

    boolean hasChildrenByParentId(Long parentId, Long tenantId);

    FeatureModel save(FeatureModel feature);

    void setActive(Long id, Long tenantId, boolean active);

    void deleteByIdAndTenantId(Long id, Long tenantId);
}
