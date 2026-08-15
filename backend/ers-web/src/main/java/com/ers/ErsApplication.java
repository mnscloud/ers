package com.ers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ErsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErsApplication.class, args);
    }
}
