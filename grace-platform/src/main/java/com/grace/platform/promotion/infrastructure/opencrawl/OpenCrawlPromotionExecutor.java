package com.grace.platform.promotion.infrastructure.opencrawl;

import com.grace.platform.promotion.domain.*;
import com.grace.platform.promotion.domain.vo.PromotionCopy;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.encryption.EncryptionService;
import com.grace.platform.shared.infrastructure.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * OpenCrawl 推广执行器
 * <p>
 * 实现 PromotionExecutor 接口，使用 OpenCrawl 适配器执行推广任务。
 * 支持解密渠道 API Key，构建 OpenCrawl 请求并处理响应结果。
 * </p>
 */
@Component
public class OpenCrawlPromotionExecutor implements PromotionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(OpenCrawlPromotionExecutor.class);

    private final OpenCrawlAdapter openCrawlAdapter;
    private final EncryptionService encryptionService;

    public OpenCrawlPromotionExecutor(OpenCrawlAdapter openCrawlAdapter, 
                                      EncryptionService encryptionService) {
        this.openCrawlAdapter = openCrawlAdapter;
        this.encryptionService = encryptionService;
    }

    @Override
    public String channelType() {
        return "opencrawl";
    }

    @Override
    public PromotionResult execute(PromotionCopy copy, PromotionChannel channel) {
        logger.info("Executing OpenCrawl promotion for channel: {}", channel.getName());

        // 1. 从 channel 解密 API Key
        String decryptedApiKey = channel.getDecryptedApiKey(encryptionService);
        if (decryptedApiKey == null || decryptedApiKey.isBlank()) {
            String errorMessage = "Channel API Key is not configured: " + channel.getName();
            logger.error(errorMessage);
            return new PromotionResult(PromotionStatus.FAILED, null, errorMessage);
        }

        // 2. 构建 OpenCrawl API 请求
        OpenCrawlRequest request = new OpenCrawlRequest(
            channel.getChannelUrl(),
            decryptedApiKey,
            copy.promotionTitle(),
            copy.promotionBody(),
            copy.recommendedMethod()
        );

        // 3. 调用 OpenCrawlAdapter
        OpenCrawlResponse response;
        try {
            response = openCrawlAdapter.execute(request);
        } catch (Exception e) {
            String errorMessage = "OpenCrawl adapter execution failed: " + e.getMessage();
            logger.error(errorMessage, e);
            throw new ExternalServiceException(
                ErrorCode.OPENCRAWL_EXECUTION_FAILED,
                "OpenCrawl",
                errorMessage
            );
        }

        // 4. 处理响应结果
        if (response.isSuccess()) {
            logger.info("OpenCrawl promotion completed successfully for channel: {}, resultUrl: {}", 
                channel.getName(), response.getResultUrl());
            return new PromotionResult(PromotionStatus.COMPLETED, response.getResultUrl(), null);
        } else {
            String errorMessage = response.getErrorMessage() != null ? 
                response.getErrorMessage() : "OpenCrawl execution failed";
            logger.warn("OpenCrawl promotion failed for channel: {}, error: {}", 
                channel.getName(), errorMessage);
            return new PromotionResult(PromotionStatus.FAILED, null, errorMessage);
        }
    }
}
