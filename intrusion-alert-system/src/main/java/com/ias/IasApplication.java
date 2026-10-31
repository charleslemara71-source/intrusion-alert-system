package com.ias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // lets the log collector run every few seconds
public class IasApplication {

    public static void main(String[] args) {
        SpringApplication.run(IasApplication.class, args);
    }
}