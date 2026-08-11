package com.willthx.common.model.exception;

import com.willthx.common.model.enums.IErrorLevel;
import com.willthx.common.model.enums.ModelType;

/**
 * 服務特定錯誤類型介面。
 * 每個服務定義一個 Enum 實作此介面（例如 UaaErrorType、IotCoreErrorType）。
 */
public interface BaseErrorType {

    /** 五位數錯誤代碼（例如 "00001"） */
    String getCustomErrorCode();

    /** 錯誤簡要描述 */
    String getMemo();

    /** 錯誤嚴重等級 */
    IErrorLevel getIErrorLevel();

    /** 所屬模組 */
    ModelType getModelType();
}
