package com.grace.platform.promotion.infrastructure.opencrawl;

import com.grace.platform.promotion.domain.PromotionMethod;

/**
 * OpenCrawl API 请求对象
 * <p>
 * 封装调用 OpenCrawl Agentic API 所需的参数。
 * </p>
 */
public class OpenCrawlRequest {

    private final String channelUrl;
    private final String apiKey;
    private final String promotionTitle;
    private final String promotionBody;
    private final PromotionMethod method;

    public OpenCrawlRequest(String channelUrl, String apiKey, String promotionTitle, 
                            String promotionBody, PromotionMethod method) {
        this.channelUrl = channelUrl;
        this.apiKey = apiKey;
        this.promotionTitle = promotionTitle;
        this.promotionBody = promotionBody;
        this.method = method;
    }

    public String getChannelUrl() {
        return channelUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getPromotionTitle() {
        return promotionTitle;
    }

    public String getPromotionBody() {
        return promotionBody;
    }

    public PromotionMethod getMethod() {
        return method;
    }

    @Override
    public String toString() {
        return String.format("OpenCrawlRequest[channelUrl=%s, method=%s]", channelUrl, method);
    }
}
