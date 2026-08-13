package com.willthx.uaa.application.api.dto.auth;

import com.willthx.uaa.application.api.dto.feature.UaaFeatureRs;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UaaLoginRs {
    private String           accessToken;
    private String           refreshToken;
    private Long             userId;
    private String           account;
    private String           displayName;
    private String           role;
    private Long             tenantId;
    private List<String>     stationIds;
    private List<UaaFeatureRs> features;
}
