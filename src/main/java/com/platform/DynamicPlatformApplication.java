package com.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DynamicPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(DynamicPlatformApplication.class, args);
    }
}
