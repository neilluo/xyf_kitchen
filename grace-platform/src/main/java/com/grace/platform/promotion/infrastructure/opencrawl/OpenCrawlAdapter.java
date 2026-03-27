package com.grace.platform.promotion.infrastructure.opencrawl;

/**
 * OpenCrawl 适配器接口
 * <p>
 * 定义与 OpenCrawl Agentic API 交互的契约，负责将推广请求发送到
 * OpenCrawl 服务并处理响应。
 * </p>
 */
public interface OpenCrawlAdapter {

    /**
     * 执行 OpenCrawl 推广请求
     *
     * @param request 推广请求参数
     * @return 推广执行结果
     */
    OpenCrawlResponse execute(OpenCrawlRequest request);
}
