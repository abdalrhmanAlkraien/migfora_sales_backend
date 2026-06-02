package com.migfora.sales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling       // ← add this
//@EnableJpaAuditing
public class SalesBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesBackendApplication.class, args);
    }

}
