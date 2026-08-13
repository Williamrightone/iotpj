package com.willthx.saas.bootstrap.advice;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.saas.exception.UpstreamApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * saas-bff 專屬例外處理。
 * GlobalExceptionHandler (common-web) 處理 AbstractServiceException；
 * 本類別補充處理上游 uaa-service 傳回的業務錯誤。
 */
@Slf4j
@RestControllerAdvice
public class SaasBffAdvice {

    @ExceptionHandler(UpstreamApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleUpstream(UpstreamApiException ex) {
        log.warn("[Upstream] code={} msg={}", ex.getCode(), ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }
}
