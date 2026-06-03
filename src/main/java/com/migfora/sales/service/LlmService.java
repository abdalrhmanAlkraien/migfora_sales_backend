package com.migfora.sales.service;

import com.migfora.sales.config.AiConfig;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/06/2026
 * @Time: 4:07 AM
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LlmService {


    private final AiConfig aiConfig;
    private final ChatModel chatModel;

    public String generate(String prompt) {
        log.info("[LLM] Calling | provider={} model={}",
                aiConfig.getProvider(), aiConfig.getQubridModel());

        ChatResponse response = chatModel.chat(UserMessage.from(prompt));

        int inputTokens  = response.tokenUsage() != null
                ? response.tokenUsage().inputTokenCount()  : -1;
        int outputTokens = response.tokenUsage() != null
                ? response.tokenUsage().outputTokenCount() : -1;

        log.info("[LLM] Done | inputTokens={} outputTokens={} totalTokens={}",
                inputTokens, outputTokens, inputTokens + outputTokens);

        return response.aiMessage().text();
    }
}
