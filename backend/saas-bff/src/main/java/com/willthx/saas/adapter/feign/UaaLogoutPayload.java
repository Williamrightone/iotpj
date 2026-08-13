package com.willthx.saas.adapter.feign;

public record UaaLogoutPayload(String accessJti, long accessRemainingSeconds, String refreshJti) {}
