package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class PessimisticLockTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(PessimisticLockTestApplication.class, args);
    }
}