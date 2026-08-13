package com.willthx.uaa.domain.service;

import com.willthx.uaa.domain.model.LoginResult;

/**
 * 認證領域服務介面（Input Port）。
 */
public interface AuthService {

    LoginResult login(String account, String password);

    void logout(String accessJti, long accessRemainingSeconds, String refreshJti);

    /** 驗 refresh token，核發新 access token；回傳新 accessToken 字串 */
    String refresh(String refreshToken);
}
