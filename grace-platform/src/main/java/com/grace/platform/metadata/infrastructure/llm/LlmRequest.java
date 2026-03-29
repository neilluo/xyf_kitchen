package com.grace.platform.metadata.infrastructure.llm;

import com.grace.platform.video.domain.ImageFrame;

import java.util.List;

/**
 * LLM请求参数封装。
 * <p>
 * 支持纯文本和多模态（文本+图像）两种模式。
 * 多模态模式下，imageFrames 字段包含 Base64 编码的图像帧。
 *
 * @param model         模型名称（如 qwen-plus, qwen3.5-plus）
 * @param systemPrompt  系统角色设定提示词
 * @param userPrompt    用户输入提示词
 * @param temperature   采样温度（0.0-1.0）
 * @param maxTokens     最大生成token数
 * @param imageFrames   图像帧列表（多模态模式），null 表示纯文本模式
 */
public record LlmRequest(
    String model,
    String systemPrompt,
    String userPrompt,
    double temperature,
    int maxTokens,
    List<ImageFrame> imageFrames
) {

    /**
     * 判断是否为多模态请求。
     *
     * @return 如果 imageFrames 不为 null 且不为空，返回 true
     */
    public boolean isMultimodal() {
        return imageFrames != null && !imageFrames.isEmpty();
    }

    /**
     * 创建纯文本请求（向后兼容）。
     *
     * @param model        模型名称
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @param temperature  采样温度
     * @param maxTokens    最大token数
     * @return 纯文本 LLM 请求
     */
    public static LlmRequest textOnly(
            String model,
            String systemPrompt,
            String userPrompt,
            double temperature,
            int maxTokens) {
        return new LlmRequest(model, systemPrompt, userPrompt, temperature, maxTokens, null);
    }

    /**
     * 创建多模态请求。
     *
     * @param model        模型名称
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @param temperature  采样温度
     * @param maxTokens    最大token数
     * @param imageFrames  图像帧列表
     * @return 多模态 LLM 请求
     */
    public static LlmRequest multimodal(
            String model,
            String systemPrompt,
            String userPrompt,
            double temperature,
            int maxTokens,
            List<ImageFrame> imageFrames) {
        return new LlmRequest(model, systemPrompt, userPrompt, temperature, maxTokens, imageFrames);
    }
}
