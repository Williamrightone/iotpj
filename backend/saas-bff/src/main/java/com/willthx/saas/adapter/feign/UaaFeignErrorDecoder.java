package com.willthx.saas.adapter.feign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.willthx.saas.exception.UpstreamApiException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class UaaFeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            byte[] bytes = response.body().asInputStream().readAllBytes();
            String body = new String(bytes, StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(body);
            String code = root.path("responseCode").asText("UPSTREAM_ERROR");
            String msg  = root.path("msg").asText("Upstream service error");
            log.warn("[Feign] {} → HTTP {} code={} msg={}", methodKey, response.status(), code, msg);
            return new UpstreamApiException(code, msg);
        } catch (IOException e) {
            log.error("[Feign] Failed to parse error response from {}", methodKey, e);
            return new UpstreamApiException("UPSTREAM_ERROR", "Upstream service error");
        }
    }
}
