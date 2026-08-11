package com.willthx.common.model.rest;

import lombok.Getter;

/**
 * 所有服務 REST 端點的統一回應封裝。
 *
 * <p>規則：
 * <ul>
 *   <li>所有 Controller 方法回傳 {@code ResponseEntity<ApiResponse<T>>}</li>
 *   <li>成功回應使用 {@link #success()} 或 {@link #success(Object)}</li>
 *   <li>錯誤回應由 GlobalExceptionHandler 建構，data 永遠為 null</li>
 *   <li>字面值 "00000" 不得出現在此類別以外</li>
 * </ul>
 */
@Getter
public class ApiResponse<T> {

    public static final String SUCCESS_CODE = "00000";
    public static final String SUCCESS_MSG  = "ok";

    private final String responseCode;
    private final String msg;
    private final T      data;

    private ApiResponse(String responseCode, String msg, T data) {
        this.responseCode = responseCode;
        this.msg          = msg;
        this.data         = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MSG, data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MSG, null);
    }

    public static <T> ApiResponse<T> error(String code, String msg) {
        return new ApiResponse<>(code, msg, null);
    }

    public boolean isSuccess() {
        return SUCCESS_CODE.equals(responseCode);
    }
}
