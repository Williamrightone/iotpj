package com.willthx.common.model.exception;

import com.willthx.common.model.enums.IErrorLevel;
import com.willthx.common.model.enums.ModelType;
import lombok.Getter;

/**
 * 所有自定義服務例外的基底類別。
 * 子類別（例如 UaaException）定義各自的 ErrorType Enum 並繼承此類別。
 */
@Getter
public class AbstractServiceException extends RuntimeException {

    private final String      errorCode;
    private final IErrorLevel errorLevel;
    private final ModelType   modelType;
    private final String      memo;

    public AbstractServiceException(BaseErrorType errorType, String exceptionMsg) {
        super(exceptionMsg);
        this.errorCode  = errorType.getCustomErrorCode();
        this.errorLevel = errorType.getIErrorLevel();
        this.modelType  = errorType.getModelType();
        this.memo       = errorType.getMemo();
    }
}
