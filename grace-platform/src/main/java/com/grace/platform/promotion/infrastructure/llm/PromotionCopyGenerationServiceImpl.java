package com.grace.platform.promotion.infrastructure.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grace.platform.metadata.domain.VideoMetadata;
import com.grace.platform.metadata.infrastructure.llm.LlmRequest;
import com.grace.platform.metadata.infrastructure.llm.LlmResponse;
import com.grace.platform.metadata.infrastructure.llm.LlmService;
import com.grace.platform.promotion.domain.ChannelType;
import com.grace.platform.promotion.domain.PromotionChannel;
import com.grace.platform.promotion.domain.PromotionCopyGenerationService;
import com.grace.platform.promotion.domain.PromotionMethod;
import com.grace.platform.promotion.domain.vo.PromotionCopy;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.domain.id.ChannelId;
import com.grace.platform.shared.infrastructure.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 推广文案生成服务实现。
 * <p>
 * 复用 Metadata 上下文的 LlmService，根据渠道类型选择不同的 Prompt 模板，
 * 生成适合各渠道风格的推广文案。
 * </p>
 *
 * @see ChannelType
 */
@Component
public class PromotionCopyGenerationServiceImpl implements PromotionCopyGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PromotionCopyGenerationServiceImpl.class);

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    @Value("${grace.llm.model:qwen-plus}")
    private String model;

    @Value("${grace.llm.promotion.temperature:0.7}")
    private double temperature;

    @Value("${grace.llm.promotion.max-tokens:2048}")
    private int maxTokens;

    // Prompt 模板表 - 按 ChannelType 区分
    private static final String SYSTEM_PROMPT_TEMPLATE = """
        你是一位专业的美食内容推广专家，擅长根据视频元数据和渠道特点生成精准、吸引人的推广文案。
        你的任务是生成适合特定渠道的推广内容，包括标题、正文、推荐发布方式及其理由。
        请确保输出为有效的 JSON 格式，包含以下字段：
        - title: 推广标题（字符串）
        - body: 推广正文（字符串）
        - method: 推荐发布方式，可选值为 POST、COMMENT、SHARE（字符串）
        - reason: 推荐此方式的理由说明（字符串）
        """;

    private static final String SOCIAL_MEDIA_PROMPT = """
        渠道类型：社交媒体（SOCIAL_MEDIA）
        风格要求：
        - 文案必须简短精炼，适合快速传播
        - 总字符数（标题+正文）不超过280字符
        - 必须包含3-5个热门话题标签（#标签格式）
        - 语言活泼、有互动性，善用表情符号
        - 突出视频最精彩的部分，制造悬念或情感共鸣
        
        视频标题：%s
        视频描述：%s
        视频标签：%s
        视频URL：%s
        
        请生成适合社交媒体传播的推广文案。
        """;

    private static final String FORUM_PROMPT = """
        渠道类型：论坛（FORUM）
        风格要求：
        - 采用结构化排版，使用小标题和段落分隔
        - 内容深入详细，适合深度讨论
        - 引导用户互动和评论，可以提出问题或邀请分享经验
        - 语言专业但不失亲切，体现专业性
        - 可以包含制作技巧、食材选择建议等内容
        
        视频标题：%s
        视频描述：%s
        视频标签：%s
        视频URL：%s
        
        请生成适合论坛讨论的推广文案。
        """;

    private static final String BLOG_PROMPT = """
        渠道类型：博客（BLOG）
        风格要求：
        - 包含视频亮点摘要，突出核心内容
        - 提供观看链接，鼓励点击观看完整视频
        - 可以包含制作背景故事或个人感悟
        - 语言流畅自然，有阅读价值
        - 适合SEO优化，包含关键词
        
        视频标题：%s
        视频描述：%s
        视频标签：%s
        视频URL：%s
        
        请生成适合博客引流的推广文案。
        """;

    private static final String OTHER_PROMPT = """
        渠道类型：通用（OTHER）
        风格要求：
        - 生成通用推广文案，适用于各种渠道
        - 突出视频核心卖点和亮点
        - 平衡信息量与吸引力
        - 语言通顺，易于理解
        
        视频标题：%s
        视频描述：%s
        视频标签：%s
        视频URL：%s
        
        请生成通用的推广文案。
        """;

    public PromotionCopyGenerationServiceImpl(LlmService llmService, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    @Override
    public PromotionCopy generate(VideoMetadata metadata, PromotionChannel channel, String videoUrl) {
        log.debug("Generating promotion copy for channel: {}, video: {}",
            channel.getName(), metadata.getVideoId());

        // 1. 根据 ChannelType 选择 Prompt 模板
        String userPrompt = buildPrompt(channel.getType(), metadata, videoUrl);

        // 2. 构建 LLM 请求
        LlmRequest llmRequest = new LlmRequest(
            model,
            SYSTEM_PROMPT_TEMPLATE,
            userPrompt,
            temperature,
            maxTokens
        );

        // 3. 调用 LLM
        LlmResponse llmResponse = llmService.complete(llmRequest);
        log.debug("LLM response received, tokens used: prompt={}, completion={}",
            llmResponse.promptTokens(), llmResponse.completionTokens());

        // 4. 解析返回的 JSON
        ParsedResult result = parseResponse(llmResponse.content());

        // 5. 构建 PromotionCopy
        PromotionCopy promotionCopy = new PromotionCopy(
            channel.getId(),
            channel.getName(),
            channel.getType().name(),
            result.title,
            result.body,
            result.method,
            result.reason
        );

        log.info("Promotion copy generated successfully for channel: {}, method: {}",
            channel.getName(), result.method);

        return promotionCopy;
    }

    /**
     * 根据渠道类型构建对应的 Prompt
     */
    private String buildPrompt(ChannelType channelType, VideoMetadata metadata, String videoUrl) {
        String title = metadata.getTitle() != null ? metadata.getTitle() : "";
        String description = metadata.getDescription() != null ? metadata.getDescription() : "";
        String tags = metadata.getTags() != null ? String.join(", ", metadata.getTags()) : "";

        String promptTemplate = switch (channelType) {
            case SOCIAL_MEDIA -> SOCIAL_MEDIA_PROMPT;
            case FORUM -> FORUM_PROMPT;
            case BLOG -> BLOG_PROMPT;
            case OTHER -> OTHER_PROMPT;
        };

        return String.format(promptTemplate, title, description, tags, videoUrl);
    }

    /**
     * 解析 LLM 返回的 JSON 响应
     */
    private ParsedResult parseResponse(String content) {
        try {
            // 清理可能的 markdown 代码块标记
            String jsonContent = content.trim();
            if (jsonContent.startsWith("```json")) {
                jsonContent = jsonContent.substring(7);
            } else if (jsonContent.startsWith("```")) {
                jsonContent = jsonContent.substring(3);
            }
            if (jsonContent.endsWith("```")) {
                jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
            }
            jsonContent = jsonContent.trim();

            JsonNode root = objectMapper.readTree(jsonContent);

            String title = getStringField(root, "title", "精彩美食视频");
            String body = getStringField(root, "body", "");
            String methodStr = getStringField(root, "method", "POST");
            String reason = getStringField(root, "reason", "适合该渠道的推广方式");

            // 解析 method 字段
            PromotionMethod method;
            try {
                method = PromotionMethod.valueOf(methodStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown promotion method from LLM: {}, defaulting to POST", methodStr);
                method = PromotionMethod.POST;
            }

            return new ParsedResult(title, body, method, reason);
        } catch (Exception e) {
            log.error("Failed to parse LLM response: {}", content, e);
            throw new ExternalServiceException(
                ErrorCode.LLM_SERVICE_UNAVAILABLE,
                "PromotionCopyGeneration",
                "Failed to parse promotion copy generation response: " + e.getMessage()
            );
        }
    }

    /**
     * 安全获取 JSON 字段值
     */
    private String getStringField(JsonNode root, String fieldName, String defaultValue) {
        JsonNode field = root.path(fieldName);
        if (field.isMissingNode() || field.isNull()) {
            return defaultValue;
        }
        return field.asText(defaultValue);
    }

    /**
     * 解析结果内部类
     */
    private record ParsedResult(
        String title,
        String body,
        PromotionMethod method,
        String reason
    ) {}
}
