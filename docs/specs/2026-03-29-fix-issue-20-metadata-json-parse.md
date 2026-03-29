# Issue #20 修复计划 - 元数据生成 JSON 解析失败

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 Qwen LLM 返回非 JSON 格式导致的元数据生成失败问题

**Architecture:** 通过优化 System Prompt 强制要求 JSON 输出，并增强后处理逻辑以处理边缘情况

**Tech Stack:** Java 21, Spring Boot, Jackson, Qwen API

---

## 问题分析

**根本原因：**
1. `SYSTEM_PROMPT` 未明确要求返回纯 JSON 格式
2. Qwen 倾向于返回自然语言文本而非 JSON
3. 后处理逻辑 `extractJsonFromMarkdown()` 无法处理纯文本响应

**错误日志：**
```
Failed to parse LLM response as JSON: Unrecognized token '根据您提供的视频文件名'
```

---

## 文件结构

| 文件 | 操作 | 说明 |
|------|------|------|
| `MetadataGenerationServiceImpl.java` | 修改 | 优化 SYSTEM_PROMPT，增强 parseLlmResponse() |
| `MetadataGenerationServiceImplTest.java` | 创建 | 单元测试 |

---

## Task 1: 优化 MetadataGenerationServiceImpl

**Files:**
- Modify: `grace-platform/src/main/java/com/grace/platform/metadata/infrastructure/llm/MetadataGenerationServiceImpl.java`

### Step 1: 优化 SYSTEM_PROMPT

修改 SYSTEM_PROMPT 常量，强制要求 JSON 输出：

```java
private static final String SYSTEM_PROMPT = """
    你是一位专业的美食视频内容运营专家。
    
    任务：根据视频信息生成适合 YouTube 发布的元数据。
    
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
    {"title":"美食制作教程","description":"详细步骤...","tags":["美食","教程"]}
    """;
```

### Step 2: 增强 parseLlmResponse() 方法

添加更健壮的 JSON 提取逻辑：

```java
private GeneratedMetadata parseLlmResponse(String content) {
    // 1. 清理内容（去除常见的前缀/后缀文本）
    String cleaned = cleanLlmResponse(content);
    
    try {
        // 2. 尝试直接解析
        JsonNode root = objectMapper.readTree(cleaned);
        return extractMetadataFromJson(root);
    } catch (JsonProcessingException e) {
        log.debug("Direct JSON parsing failed: {}", e.getMessage());
        
        // 3. 尝试从 markdown 代码块提取
        String jsonFromMarkdown = extractJsonFromMarkdown(cleaned);
        if (jsonFromMarkdown != null) {
            try {
                JsonNode root = objectMapper.readTree(jsonFromMarkdown);
                return extractMetadataFromJson(root);
            } catch (JsonProcessingException e2) {
                log.debug("Markdown JSON parsing failed: {}", e2.getMessage());
            }
        }
        
        // 4. 尝试使用正则表达式提取 JSON
        String jsonFromRegex = extractJsonWithRegex(cleaned);
        if (jsonFromRegex != null) {
            try {
                JsonNode root = objectMapper.readTree(jsonFromRegex);
                return extractMetadataFromJson(root);
            } catch (JsonProcessingException e3) {
                log.debug("Regex JSON parsing failed: {}", e3.getMessage());
            }
        }
        
        // 5. 所有尝试失败，抛出异常
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
    // 去除常见的前缀文本
    String cleaned = content.trim();
    // 去除 BOM 标记
    cleaned = cleaned.replace("\uFEFF", "");
    return cleaned;
}

private String extractJsonWithRegex(String content) {
    // 匹配 JSON 对象模式
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
        "\\{[\\s\\S]*?\"title\"[\\s\\S]*?\"description\"[\\s\\S]*?\"tags\"[\\s\\S]*?\\}"
    );
    java.util.regex.Matcher matcher = pattern.matcher(content);
    if (matcher.find()) {
        return matcher.group();
    }
    return null;
}
```

### Step 3: 提交修改

```bash
git add grace-platform/src/main/java/com/grace/platform/metadata/infrastructure/llm/MetadataGenerationServiceImpl.java
git commit -m "fix: 优化 SYSTEM_PROMPT 强制 JSON 输出，增强 JSON 解析容错能力

- 优化 SYSTEM_PROMPT，明确要求返回纯 JSON 格式
- 添加 cleanLlmResponse() 清理 LLM 响应
- 添加 extractJsonWithRegex() 使用正则提取 JSON
- 增强 parseLlmResponse() 的多层容错处理

Fixes #20"
```

---

## Task 2: 创建单元测试

**Files:**
- Create: `grace-platform/src/test/java/com/grace/platform/metadata/infrastructure/llm/MetadataGenerationServiceImplTest.java`

### Step 4: 创建测试类

```java
package com.grace.platform.metadata.infrastructure.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grace.platform.metadata.domain.VideoMetadata;
import com.grace.platform.video.domain.VideoFileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetadataGenerationServiceImplTest {

    @Mock
    private LlmService llmService;

    private ObjectMapper objectMapper;
    private MetadataGenerationServiceImpl service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new MetadataGenerationServiceImpl(llmService, objectMapper);
    }

    @Test
    void shouldParseValidJsonResponse() {
        // Given
        VideoFileInfo videoInfo = new VideoFileInfo("test.mp4", 1024, Duration.ofMinutes(5));
        String llmResponse = """
            {"title":"Test Title","description":"Test Description","tags":["tag1","tag2"]}
            """;
        
        when(llmService.complete(any())).thenReturn(
            new LlmResponse(llmResponse, 100, 50)
        );

        // When
        VideoMetadata metadata = service.generate(videoInfo, List.of());

        // Then
        assertThat(metadata.getTitle()).isEqualTo("Test Title");
        assertThat(metadata.getDescription()).isEqualTo("Test Description");
        assertThat(metadata.getTags()).containsExactly("tag1", "tag2");
    }

    @Test
    void shouldParseJsonWithMarkdownCodeBlock() {
        // Given
        VideoFileInfo videoInfo = new VideoFileInfo("test.mp4", 1024, Duration.ofSeconds(30));
        String llmResponse = """
            ```json
            {"title":"Markdown Title","description":"Markdown Description","tags":["md1","md2"]}
            ```
            """;
        
        when(llmService.complete(any())).thenReturn(
            new LlmResponse(llmResponse, 100, 50)
        );

        // When
        VideoMetadata metadata = service.generate(videoInfo, List.of());

        // Then
        assertThat(metadata.getTitle()).isEqualTo("Markdown Title");
    }

    @Test
    void shouldParseJsonWithExtraText() {
        // Given
        VideoFileInfo videoInfo = new VideoFileInfo("cake.mp4", 1024, Duration.ofMinutes(3));
        String llmResponse = """
            根据您的视频，我生成了以下元数据：
            {"title":"Cake Tutorial","description":"How to make cake","tags":["cake","baking"]}
            希望对您有帮助！
            """;
        
        when(llmService.complete(any())).thenReturn(
            new LlmResponse(llmResponse, 100, 50)
        );

        // When
        VideoMetadata metadata = service.generate(videoInfo, List.of());

        // Then
        assertThat(metadata.getTitle()).isEqualTo("Cake Tutorial");
        assertThat(metadata.getTags()).containsExactly