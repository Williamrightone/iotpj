package com.willthx.uaa.domain.service;

import com.willthx.common.model.enums.Role;
import com.willthx.uaa.domain.model.FeatureModel;

import java.util.List;
import java.util.Map;

/**
 * 功能管理與角色權限領域服務介面（Input Port）。
 */
public interface FeatureService {

    List<FeatureModel> listFeatures(Long tenantId);

    FeatureModel createFeature(Long tenantId, Long parentId, String featureCode,
                               String featureName, String route, int sortOrder);

    FeatureModel updateFeature(Long id, Long tenantId, String featureName,
                               String route, int sortOrder);

    void setActive(Long id, Long tenantId, boolean active);

    void deleteFeature(Long id, Long tenantId);

    /** 回傳 leaf features 與每個角色的 featureId 清單（用於 role-permissions 頁） */
    Map<String, Object> getRolePermissions(Long tenantId);

    void updateRolePermissions(Long tenantId, String role, List<Long> featureIds);

    /** 依 role 回傳已過濾的功能平坦清單（登入時使用） */
    List<FeatureModel> getFeatureTree(Long tenantId, Role role);
}
