package com.willthx.uaa.domain.service.impl;

import com.willthx.common.model.enums.Role;
import com.willthx.uaa.domain.model.UserModel;
import com.willthx.uaa.domain.model.UserStatus;
import com.willthx.uaa.domain.port.TokenPort;
import com.willthx.uaa.domain.port.UserRepository;
import com.willthx.uaa.domain.service.UserService;
import com.willthx.uaa.exception.UaaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final TokenPort      tokenPort;

    @Override
    public List<UserModel> listUsers(Long tenantId) {
        return userRepository.findAllByTenantId(tenantId);
    }

    @Override
    public UserModel getUser(Long id, Long tenantId) {
        return userRepository.findById(id)
                .filter(u -> tenantId.equals(u.getTenantId()))
                .orElseThrow(() -> new UaaException(UaaException.UaaErrorType.USER_NOT_FOUND));
    }

    @Override
    @Transactional
    public UserModel createUser(Long tenantId, String account, String displayName,
                                Role role, String rawPassword, List<String> stationIds) {
        UserModel newUser = UserModel.builder()
                .account(account)
                .displayName(displayName)
                .role(role)
                .tenantId(tenantId)
                .status(UserStatus.ACTIVE)
                .build();
        UserModel saved = userRepository.save(newUser, rawPassword);
        if (stationIds != null && !stationIds.isEmpty()) {
            userRepository.replaceStations(saved.getId(), tenantId, stationIds);
        }
        return saved;
    }

    @Override
    @Transactional
    public UserModel updateUser(Long id, Long tenantId, String displayName,
                                Role role, List<String> stationIds) {
        UserModel existing = userRepository.findById(id)
                .filter(u -> tenantId.equals(u.getTenantId()))
                .orElseThrow(() -> new UaaException(UaaException.UaaErrorType.USER_NOT_FOUND));

        UserModel updated = UserModel.builder()
                .id(existing.getId())
                .account(existing.getAccount())
                .displayName(displayName)
                .role(role)
                .tenantId(tenantId)
                .status(existing.getStatus())
                .lastLoginAt(existing.getLastLoginAt())
                .build();
        UserModel saved = userRepository.save(updated, null);

        userRepository.replaceStations(id, tenantId, stationIds != null ? stationIds : List.of());
        tokenPort.evictStations(id);

        return saved;
    }

    @Override
    @Transactional
    public void disableUser(Long id, Long tenantId) {
        UserModel user = userRepository.findById(id)
                .filter(u -> tenantId.equals(u.getTenantId()))
                .orElseThrow(() -> new UaaException(UaaException.UaaErrorType.USER_NOT_FOUND));

        if (user.getRole() == Role.ADMIN) {
            long remaining = userRepository.countActiveAdminsExcluding(tenantId, id);
            if (remaining == 0) {
                throw new UaaException(UaaException.UaaErrorType.LAST_ADMIN_DISABLE);
            }
        }
        userRepository.updateStatus(id, tenantId, UserStatus.DISABLED);
    }

    @Override
    public List<String> getStations(Long userId, Long tenantId) {
        userRepository.findById(userId)
                .filter(u -> tenantId.equals(u.getTenantId()))
                .orElseThrow(() -> new UaaException(UaaException.UaaErrorType.USER_NOT_FOUND));
        return userRepository.findStationIds(userId, tenantId);
    }

    @Override
    @Transactional
    public void updateStations(Long userId, Long tenantId, List<String> stationIds) {
        userRepository.findById(userId)
                .filter(u -> tenantId.equals(u.getTenantId()))
                .orElseThrow(() -> new UaaException(UaaException.UaaErrorType.USER_NOT_FOUND));
        userRepository.replaceStations(userId, tenantId, stationIds != null ? stationIds : List.of());
        tokenPort.evictStations(userId);
    }
}
