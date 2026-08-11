package com.willthx.uaa.domain.port;

import com.willthx.uaa.domain.model.UserModel;

import java.util.Optional;

/**
 * 使用者儲存庫輸出埠（Output Port）。
 * Adapter 層（UserRepositoryImpl）實作此介面。
 * 所有方法操作 Domain Model，不暴露 Entity。
 */
public interface UserRepository {

    Optional<UserModel> findByUsername(String username);

    Optional<UserModel> findById(Long id);

    /**
     * 儲存使用者（新增或更新）。
     * 密碼雜湊由 Adapter 在持久化前處理。
     *
     * @param user        領域模型
     * @param rawPassword 明文密碼（新增時非空；更新時為 null 表示不變更）
     * @return 儲存後的領域模型
     */
    UserModel save(UserModel user, String rawPassword);

    void updateLastLogin(Long userId);
}
