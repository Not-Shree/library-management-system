package com.library.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Library Management & Fine Calculator backend.
 * EnableScheduling is used for the due-date / overdue notification job
 * added in later stages.
 */
@SpringBootApplication
@EnableScheduling
public class LmsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LmsBackendApplication.class, args);
    }
}
