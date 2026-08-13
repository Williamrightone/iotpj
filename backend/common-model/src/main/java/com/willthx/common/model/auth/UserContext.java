package com.willthx.common.model.auth;

import com.willthx.common.model.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * BFF 請求內的呼叫者身份（僅存在於 saas-bff ThreadLocal 中）。
 *
 * <p>業務服務不得實例化或讀取 UserContext，
 * 身份以明確的方法參數（Long userId、Long tenantId）傳遞。
 */
@Getter
@Builder
public class UserContext {

    /** 使用者 Snowflake ID */
    private final Long         userId;

    /** 登入帳號（Email 格式） */
    private final String       account;

    /** 顯示姓名 */
    private final String       displayName;

    /** 角色（ADMIN / MAINTAINER / VIEWER） */
    private final Role         role;

    /** 所屬租戶 ID */
    private final Long         tenantId;

    /** JWT ID，用於定向登出（JWT Blacklist） */
    private final String       jti;

    /** Access Token 過期時間（epoch 秒），用於設定 Blacklist TTL */
    private final long         exp;

    /**
     * 站點存取範圍（僅 MAINTAINER / VIEWER 使用）。
     * 空 List 表示可存取全部站點。
     */
    private final List<String> stationIds;
}
