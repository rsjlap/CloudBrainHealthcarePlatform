package com.neusoft.cloudbrain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CloudBrainApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudBrainApplication.class, args);
    }
}
