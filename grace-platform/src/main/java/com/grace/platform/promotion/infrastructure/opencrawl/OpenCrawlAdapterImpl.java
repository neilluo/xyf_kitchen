package com.grace.platform.promotion.infrastructure.opencrawl;

import com.grace.platform.promotion.infrastructure.config.OpenCrawlProperties;
import com.grace.platform.shared.infrastructure.log.DigestLogger;
import com.grace.platform.shared.infrastructure.log.TraceContext;
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

        String parentRpc = TraceContext.pushSpan();
        long start = System.currentTimeMillis();
        String status = "OK";
        String errCode = "-";
        String resultUrl = null;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", request.getApiKey());

            Map<String, Object> body = new HashMap<>();
            body.put("channel_url", request.getChannelUrl());
            body.put("title", request.getPromotionTitle());
            body.put("content", request.getPromotionBody());
            body.put("method", request.getMethod().name().toLowerCase());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

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
                    resultUrl = (String) responseBody.get("result_url");
                    logger.info("OpenCrawl promotion succeeded, resultUrl: {}", resultUrl);

                    DigestLogger.opencrawl("execute", request.getChannelUrl(),
                        request.getMethod().name().toLowerCase(),
                        System.currentTimeMillis() - start, status, errCode, resultUrl);

                    return OpenCrawlResponse.success(resultUrl, rawResponse);
                } else {
                    String errorMessage = (String) responseBody.get("error_message");
                    logger.warn("OpenCrawl promotion failed: {}", errorMessage);

                    DigestLogger.opencrawl("execute", request.getChannelUrl(),
                        request.getMethod().name().toLowerCase(),
                        System.currentTimeMillis() - start, "BIZ_FAIL",
                        errorMessage, null);

                    return OpenCrawlResponse.failure(errorMessage, rawResponse);
                }
            } else {
                String errorMessage = "OpenCrawl API returned non-success status: " + response.getStatusCode();
                logger.error(errorMessage);

                DigestLogger.opencrawl("execute", request.getChannelUrl(),
                    request.getMethod().name().toLowerCase(),
                    System.currentTimeMillis() - start, "BIZ_FAIL",
                    String.valueOf(response.getStatusCode()), null);

                return OpenCrawlResponse.failure(errorMessage, rawResponse);
            }
        } catch (RestClientException e) {
            String errorMessage = "Failed to call OpenCrawl API: " + e.getMessage();
            logger.error(errorMessage, e);

            DigestLogger.opencrawl("execute", request.getChannelUrl(),
                request.getMethod().name().toLowerCase(),
                System.currentTimeMillis() - start, "FAIL",
                e.getClass().getSimpleName(), null);

            return OpenCrawlResponse.failure(errorMessage, e.getMessage());
        } finally {
            TraceContext.popSpan(parentRpc);
        }
    }
}
