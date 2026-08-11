package com.willthx.common.model.enums;

/**
 * 系統角色列舉。
 * 比較時使用列舉同一性（==），不得與字串字面值比較。
 */
public enum Role {
    ADMIN,
    OPERATOR,
    VIEWER
}
