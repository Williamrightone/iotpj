package com.willthx.iotcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.willthx")
public class IotCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(IotCoreApplication.class, args);
    }
}
