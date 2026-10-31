package com.zuvo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZuvoApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZuvoApplication.class, args);
    }
}
