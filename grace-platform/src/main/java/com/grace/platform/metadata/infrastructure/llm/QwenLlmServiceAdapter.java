package com.grace.platform.metadata.infrastructure.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云通义千问 LLM 服务适配器。
 * <p>
 * 实现指数退避重试策略（1s, 2s, 4s，最多3次），
 * 失败后抛出 ExternalServiceException(9001)。
 */
@Component
public class QwenLlmServiceAdapter implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(QwenLlmServiceAdapter.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${grace.llm.api-key:}")
    private String apiKey;

    @Value("${grace.llm.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${grace.llm.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${grace.llm.retry.backoff-multiplier:2}")
    private int backoffMultiplier;

    @Value("${grace.llm.retry.initial-interval-ms:1000}")
    private long initialIntervalMs;

    public QwenLlmServiceAdapter(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        int attempt = 0;
        long backoffInterval = initialIntervalMs;

        while (attempt < maxAttempts) {
            attempt++;
            log.debug("LLM request attempt {} of {}", attempt, maxAttempts);

            try {
                return executeRequest(request);
            } catch (RestClientException e) {
                log.warn("LLM request failed on attempt {}: {}", attempt, e.getMessage());

                if (attempt >= maxAttempts) {
                    log.error("LLM request failed after {} attempts", maxAttempts);
                    throw new ExternalServiceException(
                        ErrorCode.LLM_SERVICE_UNAVAILABLE,
                        "Qwen",
                        "LLM service unavailable after " + maxAttempts + " retry attempts: " + e.getMessage()
                    );
                }

                // 指数退避等待
                try {
                    log.debug("Waiting {}ms before retry", backoffInterval);
                    Thread.sleep(backoffInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ExternalServiceException(
                        ErrorCode.LLM_SERVICE_UNAVAILABLE,
                        "Qwen",
                        "Retry interrupted: " + ie.getMessage()
                    );
                }

                // 计算下一次等待时间
                backoffInterval *= backoffMultiplier;
            }
        }

        // 不应该到达这里，但为了编译安全
        throw new ExternalServiceException(
            ErrorCode.LLM_SERVICE_UNAVAILABLE,
            "Qwen",
            "LLM request failed unexpectedly"
        );
    }

    private LlmResponse executeRequest(LlmRequest request) throws RestClientException {
        String url = baseUrl + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = buildRequestBody(request);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            String.class
        );

        return parseResponse(response.getBody());
    }

    private Map<String, Object> buildRequestBody(LlmRequest request) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", request.model());
        requestBody.put("temperature", request.temperature());
        requestBody.put("max_tokens", request.maxTokens());

        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", request.systemPrompt()),
            Map.of("role", "user", "content", request.userPrompt())
        );
        requestBody.put("messages", messages);

        return requestBody;
    }

    private LlmResponse parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode choices = root.path("choices");
            if (choices.isEmpty()) {
                throw new ExternalServiceException(
                    ErrorCode.LLM_SERVICE_UNAVAILABLE,
                    "Qwen",
                    "Empty choices in LLM response"
                );
            }

            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.path("message");
            String content = message.path("content").asText();

            JsonNode usage = root.path("usage");
            int promptTokens = usage.path("prompt_tokens").asInt(0);
            int completionTokens = usage.path("completion_tokens").asInt(0);

            return new LlmResponse(content, promptTokens, completionTokens);
        } catch (Exception e) {
            throw new ExternalServiceException(
                ErrorCode.LLM_SERVICE_UNAVAILABLE,
                "Qwen",
                "Failed to parse LLM response: " + e.getMessage()
            );
        }
    }
}
