package com.willthx.uaa.domain.service.impl;

import com.willthx.common.model.enums.Role;
import com.willthx.uaa.domain.model.UserModel;
import com.willthx.uaa.domain.model.UserStatus;
import com.willthx.uaa.domain.port.TokenPort;
import com.willthx.uaa.domain.port.UserRepository;
import com.willthx.uaa.exception.UaaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.willthx.uaa.exception.UaaException.UaaErrorType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock TokenPort      tokenPort;

    @InjectMocks
    UserServiceImpl userService;

    private static final Long TENANT_ID = 10L;
    private static final Long USER_ID   = 1L;

    private UserModel adminUser;

    @BeforeEach
    void setUp() {
        adminUser = UserModel.builder()
                .id(USER_ID).account("admin@test.com").displayName("Admin")
                .role(Role.ADMIN).tenantId(TENANT_ID).status(UserStatus.ACTIVE).build();
    }

    // ── listUsers ─────────────────────────────────────────────────────────────

    @Test
    void list_users_returns_all_by_tenant() {
        given(userRepository.findAllByTenantId(TENANT_ID)).willReturn(List.of(adminUser));

        List<UserModel> result = userService.listUsers(TENANT_ID);

        assertThat(result).hasSize(1).first().extracting(UserModel::getAccount).isEqualTo("admin@test.com");
    }

    // ── getUser ───────────────────────────────────────────────────────────────

    @Test
    void get_user_found_returns_model() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(adminUser));

        UserModel result = userService.getUser(USER_ID, TENANT_ID);

        assertThat(result.getId()).isEqualTo(USER_ID);
    }

    @Test
    void get_user_not_found_throws_UaaException_USER_NOT_FOUND() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(99L, TENANT_ID))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(USER_NOT_FOUND.getCustomErrorCode());
    }

    @Test
    void get_user_different_tenant_throws_UaaException_USER_NOT_FOUND() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> userService.getUser(USER_ID, 999L))  // wrong tenantId
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(USER_NOT_FOUND.getCustomErrorCode());
    }

    // ── createUser ────────────────────────────────────────────────────────────

    @Test
    void create_user_no_stations_returns_model() {
        UserModel saved = UserModel.builder().id(2L).account("new@test.com")
                .displayName("New").role(Role.VIEWER).tenantId(TENANT_ID).status(UserStatus.ACTIVE).build();
        given(userRepository.save(any(), eq("pass"))).willReturn(saved);

        UserModel result = userService.createUser(TENANT_ID, "new@test.com", "New", Role.VIEWER, "pass", List.of());

        assertThat(result.getAccount()).isEqualTo("new@test.com");
        verify(userRepository, never()).replaceStations(anyLong(), anyLong(), anyList());
    }

    @Test
    void create_user_with_stations_calls_replaceStations() {
        UserModel saved = UserModel.builder().id(2L).account("new@test.com")
                .displayName("New").role(Role.VIEWER).tenantId(TENANT_ID).status(UserStatus.ACTIVE).build();
        given(userRepository.save(any(), eq("pass"))).willReturn(saved);

        userService.createUser(TENANT_ID, "new@test.com", "New", Role.VIEWER, "pass", List.of("ST-1"));

        verify(userRepository).replaceStations(2L, TENANT_ID, List.of("ST-1"));
    }

    // ── updateUser ────────────────────────────────────────────────────────────

    @Test
    void update_user_found_updates_and_evicts_stations() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(adminUser));
        UserModel updated = UserModel.builder().id(USER_ID).account("admin@test.com")
                .displayName("Updated").role(Role.ADMIN).tenantId(TENANT_ID).status(UserStatus.ACTIVE).build();
        given(userRepository.save(any(), isNull())).willReturn(updated);

        UserModel result = userService.updateUser(USER_ID, TENANT_ID, "Updated", Role.ADMIN, List.of("ST-X"));

        assertThat(result.getDisplayName()).isEqualTo("Updated");
        verify(userRepository).replaceStations(USER_ID, TENANT_ID, List.of("ST-X"));
        verify(tokenPort).evictStations(USER_ID);
    }

    @Test
    void update_user_not_found_throws_UaaException_USER_NOT_FOUND() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, TENANT_ID, "X", Role.VIEWER, List.of()))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(USER_NOT_FOUND.getCustomErrorCode());
    }

    // ── disableUser ───────────────────────────────────────────────────────────

    @Test
    void disable_user_non_admin_role_disables_without_admin_check() {
        UserModel viewer = UserModel.builder().id(3L).account("v@test.com")
                .role(Role.VIEWER).tenantId(TENANT_ID).status(UserStatus.ACTIVE).build();
        given(userRepository.findById(3L)).willReturn(Optional.of(viewer));

        userService.disableUser(3L, TENANT_ID);

        verify(userRepository).updateStatus(3L, TENANT_ID, UserStatus.DISABLED);
        verify(userRepository, never()).countActiveAdminsExcluding(anyLong(), anyLong());
    }

    @Test
    void disable_user_last_admin_throws_UaaException_LAST_ADMIN_DISABLE() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(adminUser));
        given(userRepository.countActiveAdminsExcluding(TENANT_ID, USER_ID)).willReturn(0L);

        assertThatThrownBy(() -> userService.disableUser(USER_ID, TENANT_ID))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(LAST_ADMIN_DISABLE.getCustomErrorCode());
    }

    @Test
    void disable_user_not_last_admin_succeeds() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(adminUser));
        given(userRepository.countActiveAdminsExcluding(TENANT_ID, USER_ID)).willReturn(1L);

        userService.disableUser(USER_ID, TENANT_ID);

        verify(userRepository).updateStatus(USER_ID, TENANT_ID, UserStatus.DISABLED);
    }

    // ── getStations ───────────────────────────────────────────────────────────

    @Test
    void get_stations_returns_station_ids() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(adminUser));
        given(userRepository.findStationIds(USER_ID, TENANT_ID)).willReturn(List.of("ST-1", "ST-2"));

        List<String> result = userService.getStations(USER_ID, TENANT_ID);

        assertThat(result).containsExactly("ST-1", "ST-2");
    }

    @Test
    void get_stations_user_not_found_throws_UaaException_USER_NOT_FOUND() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getStations(99L, TENANT_ID))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(USER_NOT_FOUND.getCustomErrorCode());
    }

    // ── updateStations ────────────────────────────────────────────────────────

    @Test
    void update_stations_replaces_and_evicts_cache() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(adminUser));

        userService.updateStations(USER_ID, TENANT_ID, List.of("ST-NEW"));

        verify(userRepository).replaceStations(USER_ID, TENANT_ID, List.of("ST-NEW"));
        verify(tokenPort).evictStations(USER_ID);
    }

    @Test
    void update_stations_user_not_found_throws_UaaException_USER_NOT_FOUND() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateStations(99L, TENANT_ID, List.of()))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(USER_NOT_FOUND.getCustomErrorCode());
    }
}
