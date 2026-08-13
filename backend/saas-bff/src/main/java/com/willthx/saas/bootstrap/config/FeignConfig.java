package com.willthx.saas.bootstrap.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willthx.saas.adapter.feign.UaaFeignErrorDecoder;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder feignErrorDecoder(ObjectMapper objectMapper) {
        return new UaaFeignErrorDecoder(objectMapper);
    }
}
