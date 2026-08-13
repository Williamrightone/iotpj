package com.willthx.uaa.domain.model;

import java.util.List;

/**
 * 登入流程的複合結果，由 AuthService.login() 回傳。
 */
public record LoginResult(
        String            accessToken,
        String            refreshToken,
        UserModel         user,
        List<String>      stationIds,
        List<FeatureModel> features
) {}
