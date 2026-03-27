package com.grace.platform.promotion.infrastructure.opencrawl;

import com.grace.platform.promotion.infrastructure.config.OpenCrawlProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * OpenCrawl 适配器实现
 * <p>
 * 使用 RestTemplate 调用 OpenCrawl Agentic API 执行推广任务。
 * </p>
 */
@Component
public class OpenCrawlAdapterImpl implements OpenCrawlAdapter {

    private static final Logger logger = LoggerFactory.getLogger(OpenCrawlAdapterImpl.class);

    private final RestTemplate restTemplate;
    private final OpenCrawlProperties properties;

    public OpenCrawlAdapterImpl(RestTemplate restTemplate, OpenCrawlProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public OpenCrawlResponse execute(OpenCrawlRequest request) {
        String url = properties.getBaseUrl() + "/promotions/execute";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", request.getApiKey());

        Map<String, Object> body = new HashMap<>();
        body.put("channel_url", request.getChannelUrl());
        body.put("title", request.getPromotionTitle());
        body.put("content", request.getPromotionBody());
        body.put("method", request.getMethod().name().toLowerCase());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            logger.debug("Calling OpenCrawl API: {}, channelUrl: {}", url, request.getChannelUrl());
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            String rawResponse = responseBody != null ? responseBody.toString() : "";

            if (response.getStatusCode().is2xxSuccessful() && responseBody != null) {
                Boolean success = (Boolean) responseBody.get("success");
                if (Boolean.TRUE.equals(success)) {
                    String resultUrl = (String) responseBody.get("result_url");
                    logger.info("OpenCrawl promotion succeeded, resultUrl: {}", resultUrl);
                    return OpenCrawlResponse.success(resultUrl, rawResponse);
                } else {
                    String errorMessage = (String) responseBody.get("error_message");
                    logger.warn("OpenCrawl promotion failed: {}", errorMessage);
                    return OpenCrawlResponse.failure(errorMessage, rawResponse);
                }
            } else {
                String errorMessage = "OpenCrawl API returned non-success status: " + response.getStatusCode();
                logger.error(errorMessage);
                return OpenCrawlResponse.failure(errorMessage, rawResponse);
            }
        } catch (RestClientException e) {
            String errorMessage = "Failed to call OpenCrawl API: " + e.getMessage();
            logger.error(errorMessage, e);
            return OpenCrawlResponse.failure(errorMessage, e.getMessage());
        }
    }
}
