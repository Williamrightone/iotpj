package com.willthx.iotcore.exception;

import com.willthx.common.model.enums.IErrorLevel;
import com.willthx.common.model.enums.ModelType;
import com.willthx.common.model.exception.AbstractServiceException;
import com.willthx.common.model.exception.BaseErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * IoTCore Service 專屬例外。
 * responseCode = "IC" + errorCode（例如 IC00001）
 */
public class IotCoreException extends AbstractServiceException {

    @Getter
    @RequiredArgsConstructor
    public enum IotCoreErrorType implements BaseErrorType {

        STATION_NOT_FOUND    ("00001", IErrorLevel.LOW,  "Station not found or does not belong to this tenant"),
        MACHINE_NOT_FOUND    ("00002", IErrorLevel.LOW,  "Machine not found or does not belong to this tenant"),
        COMPONENT_NOT_FOUND  ("00003", IErrorLevel.LOW,  "IoT component not found or does not belong to this tenant"),
        STATION_CODE_DUPLICATE ("00004", IErrorLevel.LOW, "Station code already exists in this tenant"),
        MACHINE_CODE_DUPLICATE ("00005", IErrorLevel.LOW, "Machine code already exists in this tenant"),
        COMPONENT_CODE_DUPLICATE ("00006", IErrorLevel.LOW, "Component code already exists in this scope"),
        CODE_NOT_MODIFIABLE  ("00007", IErrorLevel.LOW,  "Code field cannot be modified after creation");

        private final String      customErrorCode;
        private final IErrorLevel iErrorLevel;
        private final String      memo;

        @Override
        public ModelType getModelType() {
            return ModelType.IC;
        }
    }

    public IotCoreException(IotCoreErrorType errorType) {
        super(errorType, errorType.getMemo());
    }
}
