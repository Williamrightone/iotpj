package com.willthx.common.web.exception;

import com.willthx.common.model.enums.IErrorLevel;
import com.willthx.common.model.exception.AbstractServiceException;
import com.willthx.common.model.rest.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全域例外處理器。
 *
 * <p>規則：
 * <ul>
 *   <li>Controller 本身不建立錯誤回應，一律拋出例外由此處理</li>
 *   <li>HIGH 等級以 log.error 記錄；LOW 以 log.warn 記錄</li>
 *   <li>錯誤回應的 data 永遠為 null</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("[Validation] {}", msg);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("SB00000", msg));
    }

    @ExceptionHandler(AbstractServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceException(AbstractServiceException ex) {
        String code = ex.getModelType().getCode() + ex.getErrorCode();
        if (ex.getErrorLevel() == IErrorLevel.HIGH) {
            log.error("[{}] {}", code, ex.getMessage());
        } else {
            log.warn("[{}] {}", code, ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(code, ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("[Unexpected] {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("XX99999", "Internal server error"));
    }
}
