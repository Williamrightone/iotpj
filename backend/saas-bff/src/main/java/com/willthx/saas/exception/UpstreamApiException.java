package com.willthx.saas.exception;

import lombok.Getter;

/**
 * uaa-service 回傳業務錯誤時，由 UaaFeignErrorDecoder 包裝並拋出。
 * 攜帶上游的 responseCode 與 msg，由 SaasBffAdvice 統一處理。
 */
@Getter
public class UpstreamApiException extends RuntimeException {

    private final String code;

    public UpstreamApiException(String code, String message) {
        super(message);
        this.code = code;
    }
}
