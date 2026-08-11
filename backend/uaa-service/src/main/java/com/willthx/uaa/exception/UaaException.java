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

        USER_NOT_FOUND         ("00001", IErrorLevel.LOW,  "User not found"),
        INVALID_CREDENTIALS    ("00002", IErrorLevel.LOW,  "Invalid username or password"),
        TOKEN_EXPIRED          ("00003", IErrorLevel.LOW,  "Token has expired"),
        TOKEN_INVALID          ("00004", IErrorLevel.LOW,  "Token is invalid or malformed"),
        REFRESH_TOKEN_NOT_FOUND("00005", IErrorLevel.LOW,  "Refresh token not found or expired"),
        ACCOUNT_DISABLED       ("00006", IErrorLevel.LOW,  "Account is disabled"),
        USERNAME_ALREADY_EXISTS("00007", IErrorLevel.LOW,  "Username already exists"),
        KEY_LOAD_FAILED        ("00008", IErrorLevel.HIGH, "Failed to load RSA key pair");

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
