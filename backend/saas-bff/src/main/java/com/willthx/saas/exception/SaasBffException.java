package com.willthx.saas.exception;

import com.willthx.common.model.enums.IErrorLevel;
import com.willthx.common.model.enums.ModelType;
import com.willthx.common.model.exception.AbstractServiceException;
import com.willthx.common.model.exception.BaseErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class SaasBffException extends AbstractServiceException {

    @Getter
    @RequiredArgsConstructor
    public enum SaasBffErrorType implements BaseErrorType {
        FORBIDDEN        ("00001", IErrorLevel.LOW, "Forbidden: insufficient permissions"),
        SELF_DISABLE     ("00002", IErrorLevel.LOW, "Cannot disable your own account"),
        SELF_ROLE_CHANGE ("00003", IErrorLevel.LOW, "Cannot change your own role");

        private final String      customErrorCode;
        private final IErrorLevel iErrorLevel;
        private final String      memo;

        @Override
        public ModelType getModelType() { return ModelType.SB; }
    }

    public SaasBffException(SaasBffErrorType errorType) {
        super(errorType, errorType.getMemo());
    }
}
