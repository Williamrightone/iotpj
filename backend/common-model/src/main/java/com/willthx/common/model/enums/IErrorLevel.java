package com.willthx.common.model.enums;

/**
 * 錯誤嚴重等級。
 * HIGH 等級由 GlobalExceptionHandler 以 log.error 記錄；LOW 以 log.warn 記錄。
 */
public enum IErrorLevel {
    LOW,
    HIGH
}
