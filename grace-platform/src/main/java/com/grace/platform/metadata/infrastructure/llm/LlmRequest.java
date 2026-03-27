package com.grace.platform.metadata.infrastructure.llm;

/**
 * LLM请求参数封装。
 *
 * @param model         模型名称（如 qwen-plus）
 * @param systemPrompt  系统角色设定提示词
 * @param userPrompt    用户输入提示词
 * @param temperature   采样温度（0.0-1.0）
 * @param maxTokens     最大生成token数
 */
public record LlmRequest(
    String model,
    String systemPrompt,
    String userPrompt,
    double temperature,
    int maxTokens
) {
}
