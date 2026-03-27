package com.grace.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // Enable scheduled tasks (quota retry)
public class GracePlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(GracePlatformApplication.class, args);
    }
}
