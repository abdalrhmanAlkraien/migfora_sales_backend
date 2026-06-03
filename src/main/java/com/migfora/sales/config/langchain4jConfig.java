package com.migfora.sales.config;

import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;

import java.time.Duration;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/06/2026
 * @Time: 7:21 AM
 */
@Configuration
@Log4j2
public class langchain4jConfig {

    @Bean
    public ChatModel chatModel(AiConfig aiConfig) {

        return switch (aiConfig.getProvider()) {
            case "qubrid" -> OpenAiChatModel.builder()
                    .baseUrl(aiConfig.getQubridBaseUrl()) // IMPORTANT
                    .apiKey(aiConfig.getQubridApiKey())
                    .modelName(aiConfig.getQubridModel())
                    .temperature(0.3)
                    .maxTokens(4000)
                    .timeout(Duration.ofMinutes(5))   // ← add this
                    .returnThinking(false)    // ← don't return thinking tokens in response
                    .sendThinking(false)      // ← don't send thinking field in request
                    .build();
            case "bedrock" -> BedrockChatModel.builder()
                    .region(Region.of(aiConfig.getBedrockRegion()))
                    .modelId(aiConfig.getBedrockModel())
                    .returnThinking(false)    // ← don't return thinking tokens in response
                    .sendThinking(false)      // ← don't send thinking field in request
                    .timeout(Duration.ofMinutes(5))   // ← add this
                    .build();
            default -> throw new IllegalStateException("Unexpected value: " + aiConfig.getProvider());
        };
    }
}
