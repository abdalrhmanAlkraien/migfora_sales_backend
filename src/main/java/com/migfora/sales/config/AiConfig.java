package com.migfora.sales.config;

import com.migfora.sales.entity.AiProvider;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/06/2026
 * @Time: 4:06 AM
 */
@Configuration
@Getter
public class AiConfig {

    @Value("${ai.provider:qubrid}")
    private String provider;

    @Value("${ai.qubrid.api-key:}")
    private String qubridApiKey;

    @Value("${ai.qubrid.base-url:https://api.qubrid.com/v1}")
    private String qubridBaseUrl;

    @Value("${ai.qubrid.model:gpt-4o}")
    private String qubridModel;

    @Value("${ai.bedrock.region:eu-central-1}")
    private String bedrockRegion;

    @Value("${ai.bedrock.model:anthropic.claude-3-5-sonnet-20241022-v2:0}")
    private String bedrockModel;

    @Value("${ai.qubrid.timeout-minutes:5}")
    private int qubridTimeoutMinutes;

    public Duration getQubridTimeout() {
        return Duration.ofMinutes(qubridTimeoutMinutes);
    }

    public AiProvider getActiveProvider() {
        return AiProvider.valueOf(provider.toUpperCase());
    }
}
