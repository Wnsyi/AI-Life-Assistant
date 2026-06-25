package com.lifeassistant.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 配置类 — DeepSeek + 对话级记忆管理
 */
@Configuration
public class AiConfig {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url}")
    private String baseUrl;

    @Value("${deepseek.model}")
    private String model;

    @Bean
    public OpenAiChatModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .temperature(0.7)
                .maxTokens(2000)
                .build();
    }

    @Bean
    public OpenAiStreamingChatModel streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .temperature(0.7)
                .maxTokens(2000)
                .build();
    }

    /**
     * 对话级记忆 — 每个 Conversation 拥有独立的 ChatMemory
     * Key: conversationId, Value: 该对话的短期记忆
     */
    @Bean
    public Map<Long, ChatMemory> conversationMemories() {
        return new ConcurrentHashMap<>();
    }

    /**
     * 获取或创建指定对话的 ChatMemory
     */
    public static ChatMemory getOrCreateMemory(Map<Long, ChatMemory> map, Long conversationId) {
        return map.computeIfAbsent(conversationId,
                id -> MessageWindowChatMemory.withMaxMessages(40));
    }
}
