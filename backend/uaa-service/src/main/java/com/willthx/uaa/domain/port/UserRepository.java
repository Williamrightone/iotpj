package com.willthx.uaa.domain.port;

import com.willthx.uaa.domain.model.UserModel;
import com.willthx.uaa.domain.model.UserStatus;

import java.util.List;
import java.util.Optional;

/**
 * 使用者儲存庫輸出埠（Output Port）。
 */
public interface UserRepository {

    /**
     * 以帳號 + 明文密碼驗證（BCrypt 比對在 Adapter 層完成）。
     * 驗證通過回傳 UserModel，失敗（帳號不存在或密碼錯誤）回傳 empty。
     */
    Optional<UserModel> authenticate(String account, String rawPassword);

    Optional<UserModel> findByAccount(String account);

    Optional<UserModel> findById(Long id);

    List<UserModel> findAllByTenantId(Long tenantId);

    UserModel save(UserModel user, String rawPassword);

    void updateLastLogin(Long userId);

    void updateStatus(Long id, Long tenantId, UserStatus status);

    /** 查詢 tenant 內排除自身後剩餘的 ACTIVE ADMIN 數量 */
    long countActiveAdminsExcluding(Long tenantId, Long excludeUserId);

    // ── 站點綁定 ──────────────────────────────────────────────────────────────

    List<String> findStationIds(Long userId, Long tenantId);

    void replaceStations(Long userId, Long tenantId, List<String> stationIds);
}
