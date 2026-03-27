package com.grace.platform.metadata.infrastructure.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grace.platform.metadata.domain.MetadataGenerationService;
import com.grace.platform.metadata.domain.MetadataSource;
import com.grace.platform.metadata.domain.VideoMetadata;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.ExternalServiceException;
import com.grace.platform.video.domain.VideoFileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 元数据生成服务实现。
 * <p>
 * 调用 LLM 服务自动生成视频元数据（标题、描述、标签）。
 * 使用美食视频内容运营专家角色设定，基于视频文件名和历史元数据风格生成。
 * </p>
 */
@Component
public class MetadataGenerationServiceImpl implements MetadataGenerationService {

    private static final Logger log = LoggerFactory.getLogger(MetadataGenerationServiceImpl.class);

    private static final String SYSTEM_PROMPT = "你是一位专业的美食视频内容运营专家。" +
        "根据提供的视频文件信息和用户历史风格，生成适合 YouTube 发布的视频元数据。";

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    @Value("${grace.llm.model:qwen-plus}")
    private String model;

    @Value("${grace.llm.temperature:0.7}")
    private double temperature;

    @Value("${grace.llm.max-tokens:2048}")
    private int maxTokens;

    public MetadataGenerationServiceImpl(LlmService llmService, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    @Override
    public VideoMetadata generate(VideoFileInfo videoInfo, List<VideoMetadata> historicalMetadata) {
        log.info("Generating metadata for video file: {}", videoInfo.fileName());

        // 1. 构建 userPrompt
        String userPrompt = buildUserPrompt(videoInfo, historicalMetadata);

        // 2. 构建 LLM 请求
        LlmRequest request = new LlmRequest(
            model,
            SYSTEM_PROMPT,
            userPrompt,
            temperature,
            maxTokens
        );

        // 3. 调用 LLM 服务
        LlmResponse response = llmService.complete(request);
        log.debug("LLM response received, promptTokens={}, completionTokens={}",
            response.promptTokens(), response.completionTokens());

        // 4. 解析 LLM 返回的 JSON
        GeneratedMetadata generated = parseLlmResponse(response.content());
        log.info("Successfully parsed metadata: title={}, tagsCount={}",
            generated.title(), generated.tags().size());

        // 5. 构建 VideoMetadata
        VideoMetadata metadata = VideoMetadata.create(
            null,  // videoId 会在应用层设置
            generated.title(),
            generated.description(),
            generated.tags(),
            MetadataSource.AI_GENERATED
        );

        // 6. 验证字段约束
        metadata.validate();

        return metadata;
    }

    /**
     * 构建用户提示词。
     *
     * @param videoInfo          视频文件信息
     * @param historicalMetadata 历史元数据列表
     * @return 用户提示词
     */
    private String buildUserPrompt(VideoFileInfo videoInfo, List<VideoMetadata> historicalMetadata) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("视频文件名：").append(videoInfo.fileName()).append("\n");

        // 添加历史标题风格参考
        if (historicalMetadata != null && !historicalMetadata.isEmpty()) {
            String historySample = historicalMetadata.stream()
                .limit(3)
                .map(VideoMetadata::getTitle)
                .collect(Collectors.joining("\n"));
            prompt.append("历史标题风格参考：\n").append(historySample).append("\n\n");
        }

        prompt.append("请生成 JSON 格式的元数据：\n");
        prompt.append("{\n");
        prompt.append("  \"title\": \"...(不超过100字符，标题要吸引人)\",\n");
        prompt.append("  \"description\": \"...(不超过5000字符，描述要详细且包含关键词)\",\n");
        prompt.append("  \"tags\": [\"标签1\", \"标签2\", ..., \"标签N\"] (5-15个标签，要相关且热门)\n");
        prompt.append("}");

        return prompt.toString();
    }

    /**
     * 解析 LLM 响应内容。
     *
     * @param content LLM 返回的 JSON 字符串
     * @return 解析后的元数据
     * @throws ExternalServiceException 如果解析失败
     */
    private GeneratedMetadata parseLlmResponse(String content) {
        try {
            // 尝试直接解析 JSON
            JsonNode root = objectMapper.readTree(content);
            return extractMetadataFromJson(root);
        } catch (JsonProcessingException e) {
            // 如果直接解析失败，尝试从 Markdown 代码块中提取 JSON
            log.debug("Direct JSON parsing failed, trying to extract from markdown code block");
            String jsonContent = extractJsonFromMarkdown(content);
            if (jsonContent != null) {
                try {
                    JsonNode root = objectMapper.readTree(jsonContent);
                    return extractMetadataFromJson(root);
                } catch (JsonProcessingException e2) {
                    log.error("Failed to parse extracted JSON: {}", e2.getMessage());
                }
            }
            throw new ExternalServiceException(
                ErrorCode.LLM_SERVICE_UNAVAILABLE,
                "LLM",
                "Failed to parse LLM response as JSON: " + e.getMessage()
            );
        }
    }

    /**
     * 从 Markdown 代码块中提取 JSON 内容。
     *
     * @param content 原始内容
     * @return 提取的 JSON 字符串，如果没有找到则返回 null
     */
    private String extractJsonFromMarkdown(String content) {
        // 尝试匹配 ```json...``` 或 ```...``` 代码块
        int startIndex = content.indexOf("```json");
        if (startIndex != -1) {
            startIndex += 7; // 跳过 ```json
        } else {
            startIndex = content.indexOf("```");
            if (startIndex != -1) {
                startIndex += 3; // 跳过 ```
            }
        }

        if (startIndex == -1) {
            return null;
        }

        int endIndex = content.lastIndexOf("```");
        if (endIndex == -1 || endIndex <= startIndex) {
            return null;
        }

        return content.substring(startIndex, endIndex).trim();
    }

    /**
     * 从 JSON 节点提取元数据。
     *
     * @param root JSON 根节点
     * @return 提取的元数据
     */
    private GeneratedMetadata extractMetadataFromJson(JsonNode root) {
        String title = getTextOrNull(root, "title");
        String description = getTextOrNull(root, "description");

        List<String> tags = List.of();
        JsonNode tagsNode = root.path("tags");
        if (tagsNode.isArray()) {
            tags = new java.util.ArrayList<>();
            for (JsonNode tag : tagsNode) {
                if (tag.isTextual()) {
                    tags.add(tag.asText());
                }
            }
        }

        return new GeneratedMetadata(title, description, tags);
    }

    /**
     * 从 JSON 节点获取文本值，如果不存在则返回 null。
     */
    private String getTextOrNull(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        return field.isTextual() ? field.asText() : null;
    }

    /**
     * LLM 生成的元数据记录。
     */
    private record GeneratedMetadata(
        String title,
        String description,
        List<String> tags
    ) {}
}
