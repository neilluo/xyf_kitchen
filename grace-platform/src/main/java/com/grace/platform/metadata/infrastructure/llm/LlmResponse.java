package com.grace.platform.metadata.infrastructure.llm;

/**
 * LLM响应结果封装。
 *
 * @param content          生成的文本内容
 * @param promptTokens     输入提示词消耗的token数
 * @param completionTokens 生成内容消耗的token数
 */
public record LlmResponse(
    String content,
    int promptTokens,
    int completionTokens
) {
}
