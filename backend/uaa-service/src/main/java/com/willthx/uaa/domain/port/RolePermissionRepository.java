package com.willthx.uaa.domain.port;

import java.util.List;
import java.util.Map;

/**
 * 角色權限儲存庫輸出埠（Output Port）。
 */
public interface RolePermissionRepository {

    /** 查詢指定角色擁有的子功能 ID 清單 */
    List<Long> findFeatureIdsByRoleAndTenantId(String role, Long tenantId);

    /** 查詢所有角色的權限（Map: role → featureIds） */
    Map<String, List<Long>> findAllByTenantId(Long tenantId);

    /** 全量替換：刪除 role 所有舊記錄再批次插入新記錄（在一個 @Transactional 中） */
    void replacePermissions(Long tenantId, String role, List<Long> featureIds);

    /** 刪除某個子功能的所有角色權限（子功能被刪除時呼叫） */
    void deleteByFeatureIdAndTenantId(Long featureId, Long tenantId);
}
