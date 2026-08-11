package com.willthx.common.model.enums;

/**
 * 模組識別列舉，用於組合錯誤代碼前綴。
 * 回傳給客戶端的 responseCode = modelType.getCode() + errorCode（五位數）。
 * 所有對模組前綴的引用必須使用此列舉，不得硬編碼字串。
 */
public enum ModelType {
    UA("UA"),   // uaa-service
    IC("IC"),   // iotcore-service
    TE("TE"),   // telemetry-service
    IA("IA"),   // iot-adapter
    SB("SB"),   // saas-bff
    RB("RB");   // realtime-bff

    private final String code;

    ModelType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
