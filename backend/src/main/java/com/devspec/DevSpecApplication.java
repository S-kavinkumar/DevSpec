package com.devspec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DevSpecApplication {
    public static void main(String[] args) {
        SpringApplication.run(DevSpecApplication.class, args);
    }
}
