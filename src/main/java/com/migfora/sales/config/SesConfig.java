package com.migfora.sales.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 02/06/2026
 * @Time: 4:24 AM
 */
@Configuration
public class SesConfig {

    @Value("${aws.region:eu-central-1}")
    private String region;

    @Bean
    public SesClient sesClient() {
        return SesClient.builder()
                .region(Region.of(region))
                .build();
    }
}
