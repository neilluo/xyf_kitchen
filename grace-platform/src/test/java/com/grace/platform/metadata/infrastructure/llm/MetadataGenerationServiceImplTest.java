package com.grace.platform.metadata.infrastructure.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grace.platform.metadata.domain.VideoMetadata;
import com.grace.platform.shared.infrastructure.exception.ExternalServiceException;
import com.grace.platform.video.domain.VideoFileInfo;
import com.grace.platform.video.domain.VideoFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @DisplayName("正常JSON响应应成功解析")
    void shouldParseValidJsonResponse() {
        VideoFileInfo videoInfo = new VideoFileInfo(
            "test.mp4", 
            1024L, 
            VideoFormat.MP4, 
            Duration.ofMinutes(5)
        );
        String llmResponse = """
            {"title":"Test Title","description":"Test Description","tags":["tag1","tag2","tag3","tag4","tag5"]}
            """;
        
        when(llmService.complete(any())).thenReturn(
            new LlmResponse(llmResponse, 100, 50)
        );

        VideoMetadata metadata = service.generate(videoInfo, List.of());

        assertThat(metadata.getTitle()).isEqualTo("Test Title");
        assertThat(metadata.getDescription()).isEqualTo("Test Description");
        assertThat(metadata.getTags()).containsExactly("tag1", "tag2", "tag3", "tag4", "tag5");
    }

    @Test
    @DisplayName("JSON被markdown代码块包裹时应成功解析")
    void shouldParseJsonWithMarkdownCodeBlock() {
        VideoFileInfo videoInfo = new VideoFileInfo(
            "test.mp4", 
            1024L, 
            VideoFormat.MP4, 
            Duration.ofSeconds(30)
        );
        String llmResponse = """
            ```json
            {"title":"Markdown Title","description":"Markdown Description","tags":["md1","md2","md3","md4","md5"]}
            ```
            """;
        
        when(llmService.complete(any())).thenReturn(
            new LlmResponse(llmResponse, 100, 50)
        );

        VideoMetadata metadata = service.generate(videoInfo, List.of());

        assertThat(metadata.getTitle()).isEqualTo("Markdown Title");
        assertThat(metadata.getDescription()).isEqualTo("Markdown Description");
    }

    @Test
    @DisplayName("JSON前后包含额外文本时应成功解析")
    void shouldParseJsonWithExtraText() {
        VideoFileInfo videoInfo = new VideoFileInfo(
            "cake.mp4", 
            1024L, 
            VideoFormat.MP4, 
            Duration.ofMinutes(3)
        );
        String llmResponse = """
            根据您的视频，我生成了以下元数据：
            {"title":"Cake Tutorial","description":"How to make cake","tags":["cake","baking","tutorial","dessert","recipe"]}
            希望对您有帮助！
            """;
        
        when(llmService.complete(any())).thenReturn(
            new LlmResponse(llmResponse, 100, 50)
        );

        VideoMetadata metadata = service.generate(videoInfo, List.of());

        assertThat(metadata.getTitle()).isEqualTo("Cake Tutorial");
        assertThat(metadata.getTags()).containsExactly("cake", "baking", "tutorial", "dessert", "recipe");
    }

    @Test
    @DisplayName("JSON包含BOM标记时应成功解析")
    void shouldParseJsonWithBomMarker() {
        VideoFileInfo videoInfo = new VideoFileInfo(
            "test.mp4", 
            1024L, 
            VideoFormat.MP4, 
            Duration.ofMinutes(2)
        );
        String llmResponse = "\uFEFF{\"title\":\"BOM Title\",\"description\":\"BOM Description\",\"tags\":[\"t1\",\"t2\",\"t3\",\"t4\",\"t5\"]}";
        
        when(llmService.complete(any())).thenReturn(
            new LlmResponse(llmResponse, 100, 50)
        );

        VideoMetadata metadata = service.generate(videoInfo, List.of());

        assertThat(metadata.getTitle()).isEqualTo("BOM Title");
    }

    @Test
    @DisplayName("markdown代码块无json标记时应成功解析")
    void shouldParseJsonWithPlainMarkdownCodeBlock() {
        VideoFileInfo videoInfo = new VideoFileInfo(
            "test.mp4", 
            1024L, 
            VideoFormat.MP4, 
            Duration.ofMinutes(1)
        );
        String llmResponse = """
            ```
            {"title":"Plain Block Title","description":"Plain Block Description","tags":["p1","p2","p3","p4","p5"]}
            ```
            """;
        
        when(llmService.complete(any())).thenReturn(
            new LlmResponse(llmResponse, 100, 50)
        );

        VideoMetadata metadata = service.generate(videoInfo, List.of());

        assertThat(metadata.getTitle()).isEqualTo("Plain Block Title");
    }

    @Test
    @DisplayName("完全无效响应应抛出ExternalServiceException")
    void shouldThrowExceptionForCompletelyInvalidResponse() {
        VideoFileInfo videoInfo = new VideoFileInfo(
            "test.mp4", 
            1024L, 
            VideoFormat.MP4, 
            Duration.ofMinutes(1)
        );
        String llmResponse = "这是一个完全无效的响应，没有任何JSON内容。";
        
        when(llmService.complete(any())).thenReturn(
            new LlmResponse(llmResponse, 100, 50)
        );

        assertThatThrownBy(() -> service.generate(videoInfo, List.of()))
            .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    @DisplayName("空响应应抛出ExternalServiceException")
    void shouldThrowExceptionForEmptyResponse() {
        VideoFileInfo videoInfo = new VideoFileInfo(
            "test.mp4", 
            1024L, 
            VideoFormat.MP4, 
            Duration.ofMinutes(1)
        );
        String llmResponse = "";
        
        when(llmService.complete(any())).thenReturn(
            new LlmResponse(llmResponse, 100, 50)
        );

        assertThatThrownBy(() -> service.generate(videoInfo, List.of()))
            .isInstanceOf(ExternalServiceException.class);
    }
}