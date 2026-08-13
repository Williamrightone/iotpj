package com.willthx.saas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.willthx")
@EnableFeignClients(basePackages = "com.willthx.saas.adapter.feign")
public class SaasBffApplication {
    public static void main(String[] args) {
        SpringApplication.run(SaasBffApplication.class, args);
    }
}
