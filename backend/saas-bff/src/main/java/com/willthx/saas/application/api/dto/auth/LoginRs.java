package com.willthx.saas.application.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.willthx.saas.application.api.dto.feature.FeatureRs;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginRs {
    private String           accessToken;
    private String           refreshToken;
    private Long             userId;
    private String           account;
    private String           displayName;
    private String           role;
    private Long             tenantId;
    private List<String>     stationIds;
    private List<FeatureRs>  features;
}
