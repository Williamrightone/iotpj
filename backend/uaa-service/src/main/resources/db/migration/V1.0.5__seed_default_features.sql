-- willThx UAA Service
-- V1.0.5 : Demo 租戶預設功能種子資料（tenant_id = 1000）
--   僅包含前端已實作的頁面；未實作功能請勿在此加入
--   正式多租戶 onboarding 應走 API，此腳本僅供 Demo / Dev 環境使用

-- ── features ─────────────────────────────────────────────────────────────────
-- 兩層結構：父群組（parent_id IS NULL）+ 子頁面（parent_id IS NOT NULL）

INSERT INTO features
    (id, tenant_id, parent_id, feature_code, feature_name, route, sort_order, is_active, created_at, updated_at)
VALUES
-- 父群組
(1000000000010001, 1000, NULL, 'GRP_OVERVIEW', '概覽',       NULL, 10, 1, NOW(3), NOW(3)),
(1000000000010002, 1000, NULL, 'GRP_USER',     '使用者管理', NULL, 20, 1, NOW(3), NOW(3)),
(1000000000010003, 1000, NULL, 'GRP_SYSTEM',   '系統設定',   NULL, 30, 1, NOW(3), NOW(3)),

-- 子頁面
(1000000000020001, 1000, 1000000000010001, 'DASHBOARD',      '儀表板', '/dashboard',        11, 1, NOW(3), NOW(3)),
(1000000000020002, 1000, 1000000000010002, 'USER_LIST',      '帳號列表', '/users',           21, 1, NOW(3), NOW(3)),
(1000000000020003, 1000, 1000000000010003, 'SYS_FEATURES',   '功能管理', '/features',        31, 1, NOW(3), NOW(3)),
(1000000000020004, 1000, 1000000000010003, 'SYS_ROLE_PERMS', '角色權限', '/role-permissions',32, 1, NOW(3), NOW(3));

-- ── role_feature_permissions ──────────────────────────────────────────────────
-- ADMIN 程式碼層直接取全部功能，不在此表設定
-- MAINTAINER / VIEWER 目前只開放 Dashboard（其餘管理頁面尚未實作）

INSERT INTO role_feature_permissions
    (id, tenant_id, role, feature_id, created_at, updated_at)
VALUES
(1000000000030001, 1000, 'MAINTAINER', 1000000000020001, NOW(3), NOW(3)),
(1000000000030002, 1000, 'VIEWER',     1000000000020001, NOW(3), NOW(3));
