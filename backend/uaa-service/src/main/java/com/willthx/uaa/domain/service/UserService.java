package com.willthx.uaa.domain.service;

import com.willthx.common.model.enums.Role;
import com.willthx.uaa.domain.model.UserModel;

import java.util.List;

/**
 * 使用者管理領域服務介面（Input Port）。
 */
public interface UserService {

    List<UserModel> listUsers(Long tenantId);

    UserModel getUser(Long id, Long tenantId);

    UserModel createUser(Long tenantId, String account, String displayName,
                         Role role, String rawPassword, List<String> stationIds);

    UserModel updateUser(Long id, Long tenantId, String displayName,
                         Role role, List<String> stationIds);

    void disableUser(Long id, Long tenantId);

    List<String> getStations(Long userId, Long tenantId);

    void updateStations(Long userId, Long tenantId, List<String> stationIds);
}
