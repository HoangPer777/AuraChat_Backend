package com.aurachat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AuraChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuraChatApplication.class, args);
    }
}
