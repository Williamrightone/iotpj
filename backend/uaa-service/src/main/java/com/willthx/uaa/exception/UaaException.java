package com.willthx.uaa.exception;

import com.willthx.common.model.enums.IErrorLevel;
import com.willthx.common.model.enums.ModelType;
import com.willthx.common.model.exception.AbstractServiceException;
import com.willthx.common.model.exception.BaseErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * UAA Service 專屬例外。
 * responseCode = "UA" + errorCode（例如 UA00001）
 */
public class UaaException extends AbstractServiceException {

    @Getter
    @RequiredArgsConstructor
    public enum UaaErrorType implements BaseErrorType {

        // ── 通用使用者 ────────────────────────────────────────────────────────
        USER_NOT_FOUND          ("00001", IErrorLevel.LOW,  "User not found"),
        INVALID_CREDENTIALS     ("00002", IErrorLevel.LOW,  "Invalid account or password"),
        TOKEN_EXPIRED           ("00003", IErrorLevel.LOW,  "Token has expired"),
        TOKEN_INVALID           ("00004", IErrorLevel.LOW,  "Token is invalid or malformed"),
        REFRESH_TOKEN_NOT_FOUND ("00005", IErrorLevel.LOW,  "Refresh token not found or expired"),
        ACCOUNT_DISABLED        ("00006", IErrorLevel.LOW,  "Account is disabled"),
        ACCOUNT_ALREADY_EXISTS  ("00007", IErrorLevel.LOW,  "Account already exists"),
        KEY_LOAD_FAILED         ("00008", IErrorLevel.HIGH, "Failed to load RSA key pair"),

        // ── SPEC-2 使用者管理 ─────────────────────────────────────────────────
        LAST_ADMIN_DISABLE      ("00009", IErrorLevel.LOW,  "Cannot disable the last active admin"),

        // ── SPEC-3 功能管理與角色權限 ──────────────────────────────────────────
        FEATURE_CODE_DUPLICATE  ("00010", IErrorLevel.LOW,  "Feature code already exists in this tenant"),
        FEATURE_NOT_FOUND       ("00011", IErrorLevel.LOW,  "Feature not found or does not belong to this tenant"),
        PARENT_HAS_CHILDREN     ("00012", IErrorLevel.LOW,  "Cannot delete parent feature that still has children"),
        FEATURE_ID_INVALID      ("00013", IErrorLevel.LOW,  "Feature id not found or does not belong to this tenant"),
        PERMISSION_LEAF_ONLY    ("00014", IErrorLevel.LOW,  "Role permissions can only be set on leaf features");

        private final String      customErrorCode;
        private final IErrorLevel iErrorLevel;
        private final String      memo;

        @Override
        public ModelType getModelType() {
            return ModelType.UA;
        }
    }

    public UaaException(UaaErrorType errorType) {
        super(errorType, errorType.getMemo());
    }
}
