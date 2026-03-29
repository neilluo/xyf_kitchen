package com.grace.platform.metadata.infrastructure.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grace.platform.metadata.domain.MetadataGenerationService;
import com.grace.platform.metadata.domain.MetadataSource;
import com.grace.platform.metadata.domain.VideoMetadata;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.ExternalServiceException;
import com.grace.platform.video.domain.ImageFrame;
import com.grace.platform.video.domain.VideoFileInfo;
import com.grace.platform.video.domain.VideoFrameExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 元数据生成服务实现。
 * <p>
 * 支持两种模式：
 * - 纯文本模式：基于文件名和历史元数据风格生成（向后兼容）
 * - 多模态模式：提取视频关键帧，基于实际视频内容生成更准确的元数据
 * <p>
 * 调用 LLM 服务自动生成视频元数据（标题、描述、标签）。
 * 使用美食视频内容运营专家角色设定。
 * </p>
 */
@Component
public class MetadataGenerationServiceImpl implements MetadataGenerationService {

    private static final Logger log = LoggerFactory.getLogger(MetadataGenerationServiceImpl.class);

    private static final String SYSTEM_PROMPT_TEXT_ONLY = """
        你是一位专业的美食视频内容运营专家。
        
        任务：根据视频文件名信息生成适合 YouTube 发布的元数据。
        
        重要要求（必须遵守）：
        1. 必须返回纯 JSON 格式，不要有任何其他文本
        2. 不要使用 markdown 代码块（```json）包裹
        3. 直接返回 JSON 字符串，不要添加任何解释或说明
        4. 确保 JSON 格式有效，可以被标准 JSON 解析器解析
        
        JSON 字段要求：
        - title: 视频标题，不超过100字符，要吸引人
        - description: 视频描述，不超过5000字符，详细且包含关键词
        - tags: 标签数组，5-15个相关且热门的标签
        
        示例输出格式（严格遵守）：
        {"title":"美食制作教程","description":"详细步骤...","tags":["美食","教程","家常菜","烹饪","食谱"]}
        """;

    private static final String SYSTEM_PROMPT_MULTIMODAL = """
        你是一位专业的美食视频内容运营专家。
        
        任务：根据你看到的视频画面内容，生成适合 YouTube 发布的元数据。
        
        你将看到视频的3个关键帧画面（开头、中间、结尾），请仔细观察画面内容：
        - 食材：识别画面中出现的食材种类和新鲜程度
        - 烹饪步骤：分析烹饪过程的关键步骤和技巧
        - 成品效果：观察最终呈现的菜品外观和特点
        - 场景风格：判断视频的拍摄风格（家常厨房、专业厨房、户外等）
        - 人物互动：注意是否有主持人、嘉宾或教学风格
        
        重要要求（必须遵守）：
        1. 必须返回纯 JSON 格式，不要有任何其他文本
        2. 不要使用 markdown 代码块（```json）包裹
        3. 直接返回 JSON 字符串，不要添加任何解释或说明
        4. 确保 JSON 格式有效，可以被标准 JSON 解析器解析
        5. 标题和描述要准确反映你看到的实际内容，不要凭空猜测
        
        JSON 字段要求：
        - title: 视频标题，不超过100字符，要吸引人且准确反映内容
        - description: 视频描述，不超过5000字符，详细描述画面内容和制作过程
        - tags: 标签数组，5-15个基于画面内容的相关标签
        
        示例输出格式（严格遵守）：
        {"title":"红烧肉制作教程","description":"详细步骤...","tags":["红烧肉","猪肉","家常菜","烹饪","食谱"]}
        """;

    private final LlmService llmService;
    private final ObjectMapper objectMapper;
    private final VideoFrameExtractor videoFrameExtractor;

    @Value("${grace.llm.model:qwen-plus}")
    private String model;

    @Value("${grace.llm.temperature:0.7}")
    private double temperature;

    @Value("${grace.llm.max-tokens:2048}")
    private int maxTokens;

    @Value("${grace.llm.multimodal.enabled:true}")
    private boolean multimodalEnabled;

    public MetadataGenerationServiceImpl(
            LlmService llmService,
            ObjectMapper objectMapper,
            VideoFrameExtractor videoFrameExtractor) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
        this.videoFrameExtractor = videoFrameExtractor;
    }

    @Override
    public VideoMetadata generate(VideoFileInfo videoInfo, List<VideoMetadata> historicalMetadata) {
        log.info("Generating metadata for video file (text-only mode): {}", videoInfo.fileName());

        String userPrompt = buildUserPrompt(videoInfo, historicalMetadata, false);

        LlmRequest request = LlmRequest.textOnly(
            model,
            SYSTEM_PROMPT_TEXT_ONLY,
            userPrompt,
            temperature,
            maxTokens
        );

        LlmResponse response = llmService.complete(request);
        log.debug("LLM response received, promptTokens={}, completionTokens={}",
            response.promptTokens(), response.completionTokens());

        GeneratedMetadata generated = parseLlmResponse(response.content());
        log.info("Successfully parsed metadata: title={}, tagsCount={}",
            generated.title(), generated.tags().size());

        VideoMetadata metadata = VideoMetadata.create(
            null,
            generated.title(),
            generated.description(),
            generated.tags(),
            MetadataSource.AI_GENERATED
        );

        metadata.validate();

        return metadata;
    }

    @Override
    public VideoMetadata generate(VideoFileInfo videoInfo, List<VideoMetadata> historicalMetadata, Path videoPath) {
        log.info("Generating metadata for video file (multimodal mode): {}", videoInfo.fileName());

        List<ImageFrame> imageFrames = extractVideoFrames(videoPath);
        log.debug("Extracted {} frames from video", imageFrames.size());

        String userPrompt = buildUserPrompt(videoInfo, historicalMetadata, true);

        LlmRequest request = LlmRequest.multimodal(
            model,
            SYSTEM_PROMPT_MULTIMODAL,
            userPrompt,
            temperature,
            maxTokens,
            imageFrames
        );

        LlmResponse response = llmService.complete(request);
        log.debug("LLM response received, promptTokens={}, completionTokens={}",
            response.promptTokens(), response.completionTokens());

        GeneratedMetadata generated = parseLlmResponse(response.content());
        log.info("Successfully parsed metadata: title={}, tagsCount={}",
            generated.title(), generated.tags().size());

        VideoMetadata metadata = VideoMetadata.create(
            null,
            generated.title(),
            generated.description(),
            generated.tags(),
            MetadataSource.AI_GENERATED
        );

        metadata.validate();

        return metadata;
    }

    private List<ImageFrame> extractVideoFrames(Path videoPath) {
        try {
            return videoFrameExtractor.extractFrames(videoPath);
        } catch (Exception e) {
            log.warn("Failed to extract video frames, falling back to text-only mode: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildUserPrompt(VideoFileInfo videoInfo, List<VideoMetadata> historicalMetadata, boolean multimodal) {
        StringBuilder prompt = new StringBuilder();

        if (multimodal) {
            prompt.append("视频文件名：").append(videoInfo.fileName()).append("\n");
            prompt.append("视频时长：").append(formatDuration(videoInfo.duration())).append("\n\n");
            prompt.append("请仔细观察以上3帧画面，分析视频内容并生成准确的元数据。\n");
        } else {
            prompt.append("视频文件名：").append(videoInfo.fileName()).append("\n");

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
        }

        return prompt.toString();
    }

    private String formatDuration(java.time.Duration duration) {
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();
        return String.format("%d:%02d", minutes, seconds);
    }

    private GeneratedMetadata parseLlmResponse(String content) {
        String cleaned = cleanLlmResponse(content);
        
        if (cleaned.isEmpty()) {
            log.error("LLM response is empty or blank after cleaning");
            throw new ExternalServiceException(
                ErrorCode.LLM_SERVICE_UNAVAILABLE,
                "LLM",
                "LLM returned empty response"
            );
        }
        
        try {
            JsonNode root = objectMapper.readTree(cleaned);
            return extractMetadataFromJson(root);
        } catch (JsonProcessingException e) {
            log.debug("Direct JSON parsing failed: {}", e.getMessage());
            
            String jsonFromMarkdown = extractJsonFromMarkdown(cleaned);
            if (jsonFromMarkdown != null) {
                try {
                    JsonNode root = objectMapper.readTree(jsonFromMarkdown);
                    return extractMetadataFromJson(root);
                } catch (JsonProcessingException e2) {
                    log.debug("Markdown JSON parsing failed: {}", e2.getMessage());
                }
            }
            
            String jsonFromRegex = extractJsonWithRegex(cleaned);
            if (jsonFromRegex != null) {
                try {
                    JsonNode root = objectMapper.readTree(jsonFromRegex);
                    return extractMetadataFromJson(root);
                } catch (JsonProcessingException e3) {
                    log.debug("Regex JSON parsing failed: {}", e3.getMessage());
                }
            }
            
            log.error("Failed to parse LLM response as JSON. Raw content: {}", content);
            throw new ExternalServiceException(
                ErrorCode.LLM_SERVICE_UNAVAILABLE,
                "LLM",
                "Failed to parse LLM response as JSON: " + e.getMessage()
            );
        }
    }

    private String cleanLlmResponse(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String cleaned = content.trim();
        cleaned = cleaned.replace("\uFEFF", "");
        return cleaned;
    }

    private String extractJsonWithRegex(String content) {
        Pattern pattern = Pattern.compile(
            "\\{[\\s\\S]*?\"title\"[\\s\\S]*?\"description\"[\\s\\S]*?\"tags\"[\\s\\S]*?\\}"
        );
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String extractJsonFromMarkdown(String content) {
        int startIndex = content.indexOf("```json");
        if (startIndex != -1) {
            startIndex += 7;
        } else {
            startIndex = content.indexOf("```");
            if (startIndex != -1) {
                startIndex += 3;
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

    private String getTextOrNull(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        return field.isTextual() ? field.asText() : null;
    }

    private record GeneratedMetadata(
        String title,
        String description,
        List<String> tags
    ) {}
}
