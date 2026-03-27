package com.grace.platform.promotion.infrastructure.opencrawl;

/**
 * OpenCrawl API 响应对象
 * <p>
 * 封装 OpenCrawl Agentic API 的返回结果。
 * </p>
 */
public class OpenCrawlResponse {

    private final boolean success;
    private final String resultUrl;
    private final String errorMessage;
    private final String rawResponse;

    public OpenCrawlResponse(boolean success, String resultUrl, String errorMessage, String rawResponse) {
        this.success = success;
        this.resultUrl = resultUrl;
        this.errorMessage = errorMessage;
        this.rawResponse = rawResponse;
    }

    /**
     * 创建成功响应
     *
     * @param resultUrl   推广结果 URL
     * @param rawResponse 原始响应内容
     * @return 成功的响应对象
     */
    public static OpenCrawlResponse success(String resultUrl, String rawResponse) {
        return new OpenCrawlResponse(true, resultUrl, null, rawResponse);
    }

    /**
     * 创建失败响应
     *
     * @param errorMessage 错误信息
     * @param rawResponse  原始响应内容
     * @return 失败的响应对象
     */
    public static OpenCrawlResponse failure(String errorMessage, String rawResponse) {
        return new OpenCrawlResponse(false, null, errorMessage, rawResponse);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getResultUrl() {
        return resultUrl;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    @Override
    public String toString() {
        return String.format("OpenCrawlResponse[success=%s, resultUrl=%s, errorMessage=%s]", 
            success, resultUrl, errorMessage);
    }
}
