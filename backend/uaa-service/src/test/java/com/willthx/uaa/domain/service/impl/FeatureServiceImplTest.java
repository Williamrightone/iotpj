package com.willthx.uaa.domain.service.impl;

import com.willthx.common.model.enums.Role;
import com.willthx.common.model.id.SnowflakeIdGenerator;
import com.willthx.uaa.domain.model.FeatureModel;
import com.willthx.uaa.domain.port.FeatureRepository;
import com.willthx.uaa.domain.port.RolePermissionRepository;
import com.willthx.uaa.exception.UaaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.willthx.uaa.exception.UaaException.UaaErrorType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureServiceImplTest {

    @Mock FeatureRepository        featureRepository;
    @Mock RolePermissionRepository rolePermissionRepository;
    @Mock SnowflakeIdGenerator     snowflakeIdGenerator;

    @InjectMocks
    FeatureServiceImpl featureService;

    private static final Long TENANT_ID  = 10L;
    private static final Long FEATURE_ID = 100L;

    private FeatureModel rootFeature;
    private FeatureModel childFeature;

    @BeforeEach
    void setUp() {
        rootFeature = FeatureModel.builder()
                .id(FEATURE_ID).tenantId(TENANT_ID).parentId(null)
                .featureCode("USER_MGMT").featureName("使用者管理")
                .sortOrder(1).active(true).build();

        childFeature = FeatureModel.builder()
                .id(200L).tenantId(TENANT_ID).parentId(FEATURE_ID)
                .featureCode("USER_LIST").featureName("使用者列表")
                .sortOrder(1).active(true).build();
    }

    // ── listFeatures ──────────────────────────────────────────────────────────

    @Test
    void list_features_returns_all_by_tenant() {
        given(featureRepository.findAllByTenantId(TENANT_ID)).willReturn(List.of(rootFeature));

        List<FeatureModel> result = featureService.listFeatures(TENANT_ID);

        assertThat(result).hasSize(1).first().extracting(FeatureModel::getFeatureCode).isEqualTo("USER_MGMT");
    }

    // ── createFeature ─────────────────────────────────────────────────────────

    @Test
    void create_feature_root_returns_saved_model() {
        given(featureRepository.existsByCodeAndTenantId("NEW_CODE", TENANT_ID)).willReturn(false);
        given(snowflakeIdGenerator.nextId()).willReturn(999L);
        FeatureModel saved = FeatureModel.builder().id(999L).tenantId(TENANT_ID)
                .featureCode("NEW_CODE").featureName("新功能").sortOrder(0).active(true).build();
        given(featureRepository.save(any())).willReturn(saved);

        FeatureModel result = featureService.createFeature(TENANT_ID, null, "NEW_CODE", "新功能", null, 0);

        assertThat(result.getFeatureCode()).isEqualTo("NEW_CODE");
        verify(featureRepository, never()).findByIdAndTenantId(anyLong(), anyLong());
    }

    @Test
    void create_feature_duplicate_code_throws_UaaException_FEATURE_CODE_DUPLICATE() {
        given(featureRepository.existsByCodeAndTenantId("DUP", TENANT_ID)).willReturn(true);

        assertThatThrownBy(() -> featureService.createFeature(TENANT_ID, null, "DUP", "X", null, 0))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(FEATURE_CODE_DUPLICATE.getCustomErrorCode());
    }

    @Test
    void create_feature_parent_not_found_throws_UaaException_FEATURE_NOT_FOUND() {
        given(featureRepository.existsByCodeAndTenantId("CODE", TENANT_ID)).willReturn(false);
        given(featureRepository.findByIdAndTenantId(99L, TENANT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> featureService.createFeature(TENANT_ID, 99L, "CODE", "X", null, 0))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(FEATURE_NOT_FOUND.getCustomErrorCode());
    }

    @Test
    void create_feature_child_with_valid_parent_returns_model() {
        given(featureRepository.existsByCodeAndTenantId("CHILD", TENANT_ID)).willReturn(false);
        given(featureRepository.findByIdAndTenantId(FEATURE_ID, TENANT_ID)).willReturn(Optional.of(rootFeature));
        given(snowflakeIdGenerator.nextId()).willReturn(200L);
        given(featureRepository.save(any())).willReturn(childFeature);

        FeatureModel result = featureService.createFeature(TENANT_ID, FEATURE_ID, "CHILD", "子功能", "/child", 1);

        assertThat(result.getParentId()).isEqualTo(FEATURE_ID);
    }

    // ── updateFeature ─────────────────────────────────────────────────────────

    @Test
    void update_feature_found_returns_updated_model() {
        given(featureRepository.findByIdAndTenantId(FEATURE_ID, TENANT_ID)).willReturn(Optional.of(rootFeature));
        FeatureModel updated = FeatureModel.builder().id(FEATURE_ID).tenantId(TENANT_ID)
                .featureCode("USER_MGMT").featureName("更新名稱").sortOrder(5).active(true).build();
        given(featureRepository.save(any())).willReturn(updated);

        FeatureModel result = featureService.updateFeature(FEATURE_ID, TENANT_ID, "更新名稱", null, 5);

        assertThat(result.getFeatureName()).isEqualTo("更新名稱");
    }

    @Test
    void update_feature_not_found_throws_UaaException_FEATURE_NOT_FOUND() {
        given(featureRepository.findByIdAndTenantId(99L, TENANT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> featureService.updateFeature(99L, TENANT_ID, "X", null, 0))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(FEATURE_NOT_FOUND.getCustomErrorCode());
    }

    // ── setActive ─────────────────────────────────────────────────────────────

    @Test
    void set_active_found_calls_repository_setActive() {
        given(featureRepository.findByIdAndTenantId(FEATURE_ID, TENANT_ID)).willReturn(Optional.of(rootFeature));

        featureService.setActive(FEATURE_ID, TENANT_ID, false);

        verify(featureRepository).setActive(FEATURE_ID, TENANT_ID, false);
    }

    @Test
    void set_active_not_found_throws_UaaException_FEATURE_NOT_FOUND() {
        given(featureRepository.findByIdAndTenantId(99L, TENANT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> featureService.setActive(99L, TENANT_ID, true))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(FEATURE_NOT_FOUND.getCustomErrorCode());
    }

    // ── deleteFeature ─────────────────────────────────────────────────────────

    @Test
    void delete_feature_parent_with_children_throws_UaaException_PARENT_HAS_CHILDREN() {
        given(featureRepository.findByIdAndTenantId(FEATURE_ID, TENANT_ID)).willReturn(Optional.of(rootFeature));
        given(featureRepository.hasChildrenByParentId(FEATURE_ID, TENANT_ID)).willReturn(true);

        assertThatThrownBy(() -> featureService.deleteFeature(FEATURE_ID, TENANT_ID))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(PARENT_HAS_CHILDREN.getCustomErrorCode());
    }

    @Test
    void delete_feature_parent_without_children_deletes_successfully() {
        given(featureRepository.findByIdAndTenantId(FEATURE_ID, TENANT_ID)).willReturn(Optional.of(rootFeature));
        given(featureRepository.hasChildrenByParentId(FEATURE_ID, TENANT_ID)).willReturn(false);

        featureService.deleteFeature(FEATURE_ID, TENANT_ID);

        verify(featureRepository).deleteByIdAndTenantId(FEATURE_ID, TENANT_ID);
        verify(rolePermissionRepository, never()).deleteByFeatureIdAndTenantId(anyLong(), anyLong());
    }

    @Test
    void delete_feature_child_clears_permissions_and_deletes() {
        given(featureRepository.findByIdAndTenantId(200L, TENANT_ID)).willReturn(Optional.of(childFeature));

        featureService.deleteFeature(200L, TENANT_ID);

        verify(rolePermissionRepository).deleteByFeatureIdAndTenantId(200L, TENANT_ID);
        verify(featureRepository).deleteByIdAndTenantId(200L, TENANT_ID);
    }

    @Test
    void delete_feature_not_found_throws_UaaException_FEATURE_NOT_FOUND() {
        given(featureRepository.findByIdAndTenantId(99L, TENANT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> featureService.deleteFeature(99L, TENANT_ID))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(FEATURE_NOT_FOUND.getCustomErrorCode());
    }

    // ── getRolePermissions ────────────────────────────────────────────────────

    @Test
    void get_role_permissions_returns_leaves_and_permissions_map() {
        given(featureRepository.findAllByTenantId(TENANT_ID)).willReturn(List.of(rootFeature, childFeature));
        given(rolePermissionRepository.findAllByTenantId(TENANT_ID))
                .willReturn(new java.util.LinkedHashMap<>(Map.of("ADMIN", List.of(200L))));

        Map<String, Object> result = featureService.getRolePermissions(TENANT_ID);

        @SuppressWarnings("unchecked")
        List<FeatureModel> leaves = (List<FeatureModel>) result.get("features");
        assertThat(leaves).hasSize(1).first().extracting(FeatureModel::getId).isEqualTo(200L);

        @SuppressWarnings("unchecked")
        Map<String, List<Long>> perms = (Map<String, List<Long>>) result.get("permissions");
        assertThat(perms).containsKey("MAINTAINER").containsKey("VIEWER");
    }

    // ── updateRolePermissions ─────────────────────────────────────────────────

    @Test
    void update_role_permissions_empty_list_calls_replace_without_validation() {
        featureService.updateRolePermissions(TENANT_ID, "VIEWER", List.of());

        verify(rolePermissionRepository).replacePermissions(TENANT_ID, "VIEWER", List.of());
        verify(featureRepository, never()).findByIdsAndTenantId(any(), anyLong());
    }

    @Test
    void update_role_permissions_invalid_feature_id_throws_UaaException_FEATURE_ID_INVALID() {
        given(featureRepository.findByIdsAndTenantId(List.of(999L), TENANT_ID)).willReturn(List.of());

        assertThatThrownBy(() -> featureService.updateRolePermissions(TENANT_ID, "VIEWER", List.of(999L)))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(FEATURE_ID_INVALID.getCustomErrorCode());
    }

    @Test
    void update_role_permissions_parent_feature_throws_UaaException_PERMISSION_LEAF_ONLY() {
        given(featureRepository.findByIdsAndTenantId(List.of(FEATURE_ID), TENANT_ID))
                .willReturn(List.of(rootFeature)); // parentId == null → is parent

        assertThatThrownBy(() -> featureService.updateRolePermissions(TENANT_ID, "VIEWER", List.of(FEATURE_ID)))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(PERMISSION_LEAF_ONLY.getCustomErrorCode());
    }

    @Test
    void update_role_permissions_valid_leaf_calls_replacePermissions() {
        given(featureRepository.findByIdsAndTenantId(List.of(200L), TENANT_ID))
                .willReturn(List.of(childFeature)); // parentId != null → leaf

        featureService.updateRolePermissions(TENANT_ID, "MAINTAINER", List.of(200L));

        verify(rolePermissionRepository).replacePermissions(TENANT_ID, "MAINTAINER", List.of(200L));
    }

    // ── getFeatureTree ────────────────────────────────────────────────────────

    @Test
    void get_feature_tree_admin_returns_all_active_sorted() {
        FeatureModel f1 = FeatureModel.builder().id(1L).tenantId(TENANT_ID).featureCode("A")
                .featureName("A").sortOrder(2).active(true).build();
        FeatureModel f2 = FeatureModel.builder().id(2L).tenantId(TENANT_ID).featureCode("B")
                .featureName("B").sortOrder(1).active(true).build();
        given(featureRepository.findActiveByTenantId(TENANT_ID)).willReturn(List.of(f1, f2));

        List<FeatureModel> result = featureService.getFeatureTree(TENANT_ID, Role.ADMIN);

        assertThat(result).extracting(FeatureModel::getSortOrder).containsExactly(1, 2);
    }

    @Test
    void get_feature_tree_non_admin_empty_leaf_ids_returns_empty() {
        given(rolePermissionRepository.findFeatureIdsByRoleAndTenantId("VIEWER", TENANT_ID))
                .willReturn(List.of());

        List<FeatureModel> result = featureService.getFeatureTree(TENANT_ID, Role.VIEWER);

        assertThat(result).isEmpty();
        verify(featureRepository, never()).findByIdsAndTenantId(any(), anyLong());
    }

    @Test
    void get_feature_tree_non_admin_returns_leaves_and_parents() {
        given(rolePermissionRepository.findFeatureIdsByRoleAndTenantId("MAINTAINER", TENANT_ID))
                .willReturn(List.of(200L));
        given(featureRepository.findByIdsAndTenantId(List.of(200L), TENANT_ID))
                .willReturn(List.of(childFeature));
        given(featureRepository.findByIdsAndTenantId(List.of(FEATURE_ID), TENANT_ID))
                .willReturn(List.of(rootFeature));

        List<FeatureModel> result = featureService.getFeatureTree(TENANT_ID, Role.MAINTAINER);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(FeatureModel::getId).containsExactlyInAnyOrder(FEATURE_ID, 200L);
    }
}
