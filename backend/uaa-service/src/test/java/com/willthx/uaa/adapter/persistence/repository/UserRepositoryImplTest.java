package com.willthx.uaa.adapter.persistence.repository;

import com.willthx.common.model.enums.Role;
import com.willthx.common.model.id.SnowflakeIdGenerator;
import com.willthx.uaa.adapter.persistence.entity.UserEntity;
import com.willthx.uaa.adapter.persistence.entity.UserStationBindingEntity;
import com.willthx.uaa.domain.model.UserModel;
import com.willthx.uaa.domain.model.UserStatus;
import com.willthx.uaa.exception.UaaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static com.willthx.uaa.exception.UaaException.UaaErrorType.ACCOUNT_ALREADY_EXISTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

    @Mock UserJpaRepository               userJpaRepository;
    @Mock UserStationBindingJpaRepository stationJpaRepository;
    @Mock PasswordEncoder                 passwordEncoder;
    @Mock SnowflakeIdGenerator            snowflakeIdGenerator;

    @InjectMocks
    UserRepositoryImpl userRepositoryImpl;

    private static final Long TENANT_ID = 10L;
    private static final Long USER_ID   = 1L;

    private UserEntity activeEntity;

    @BeforeEach
    void setUp() {
        activeEntity = new UserEntity();
        activeEntity.setId(USER_ID);
        activeEntity.setAccount("user@test.com");
        activeEntity.setDisplayName("Test User");
        activeEntity.setPasswordHash("hashed");
        activeEntity.setRole(Role.ADMIN);
        activeEntity.setTenantId(TENANT_ID);
        activeEntity.setStatus(UserStatus.ACTIVE);
    }

    // ── authenticate ──────────────────────────────────────────────────────────

    @Test
    void authenticate_wrong_password_returns_empty() {
        given(userJpaRepository.findByAccount("user@test.com")).willReturn(Optional.of(activeEntity));
        given(passwordEncoder.matches("wrong", "hashed")).willReturn(false);

        Optional<UserModel> result = userRepositoryImpl.authenticate("user@test.com", "wrong");

        assertThat(result).isEmpty();
    }

    @Test
    void authenticate_valid_credentials_returns_model() {
        given(userJpaRepository.findByAccount("user@test.com")).willReturn(Optional.of(activeEntity));
        given(passwordEncoder.matches("secret", "hashed")).willReturn(true);

        Optional<UserModel> result = userRepositoryImpl.authenticate("user@test.com", "secret");

        assertThat(result).isPresent();
        assertThat(result.get().getAccount()).isEqualTo("user@test.com");
    }

    @Test
    void authenticate_unknown_account_returns_empty() {
        given(userJpaRepository.findByAccount("unknown@test.com")).willReturn(Optional.empty());

        Optional<UserModel> result = userRepositoryImpl.authenticate("unknown@test.com", "any");

        assertThat(result).isEmpty();
    }

    // ── save (new user) ───────────────────────────────────────────────────────

    @Test
    void save_new_user_duplicate_account_throws_UaaException_ACCOUNT_ALREADY_EXISTS() {
        UserModel newUser = UserModel.builder()
                .account("dup@test.com").displayName("Dup").role(Role.VIEWER)
                .tenantId(TENANT_ID).status(UserStatus.ACTIVE).build(); // id == null

        given(userJpaRepository.existsByAccount("dup@test.com")).willReturn(true);

        assertThatThrownBy(() -> userRepositoryImpl.save(newUser, "pass"))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(ACCOUNT_ALREADY_EXISTS.getCustomErrorCode());
    }

    @Test
    void save_new_user_persists_with_encoded_password() {
        UserModel newUser = UserModel.builder()
                .account("new@test.com").displayName("New").role(Role.VIEWER)
                .tenantId(TENANT_ID).status(UserStatus.ACTIVE).build(); // id == null

        given(userJpaRepository.existsByAccount("new@test.com")).willReturn(false);
        given(snowflakeIdGenerator.nextId()).willReturn(99L);
        given(passwordEncoder.encode("pass")).willReturn("encoded");
        given(userJpaRepository.save(any())).willReturn(activeEntity);

        userRepositoryImpl.save(newUser, "pass");

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded");
        assertThat(captor.getValue().getId()).isEqualTo(99L);
    }

    // ── save (existing user) ──────────────────────────────────────────────────

    @Test
    void save_existing_user_null_password_does_not_re_encode() {
        UserModel existing = UserModel.builder()
                .id(USER_ID).account("user@test.com").displayName("Updated").role(Role.ADMIN)
                .tenantId(TENANT_ID).status(UserStatus.ACTIVE).build();

        given(userJpaRepository.findById(USER_ID)).willReturn(Optional.of(activeEntity));
        given(userJpaRepository.save(any())).willReturn(activeEntity);

        userRepositoryImpl.save(existing, null);

        verify(passwordEncoder, never()).encode(any());
    }

    // ── replaceStations ───────────────────────────────────────────────────────

    @Test
    void replace_stations_deletes_old_and_inserts_new() {
        userRepositoryImpl.replaceStations(USER_ID, TENANT_ID, List.of("ST-1", "ST-2"));

        verify(stationJpaRepository).deleteByUserIdAndTenantId(USER_ID, TENANT_ID);
        ArgumentCaptor<List<UserStationBindingEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(stationJpaRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void replace_stations_empty_list_deletes_only() {
        userRepositoryImpl.replaceStations(USER_ID, TENANT_ID, List.of());

        verify(stationJpaRepository).deleteByUserIdAndTenantId(USER_ID, TENANT_ID);
        verify(stationJpaRepository, never()).saveAll(any());
    }

    // ── countActiveAdminsExcluding ────────────────────────────────────────────

    @Test
    void count_active_admins_excluding_delegates_to_jpa() {
        given(userJpaRepository.countByTenantIdAndStatusAndRoleAndIdNot(
                TENANT_ID, "ACTIVE", "ADMIN", USER_ID)).willReturn(2L);

        long count = userRepositoryImpl.countActiveAdminsExcluding(TENANT_ID, USER_ID);

        assertThat(count).isEqualTo(2L);
    }
}
