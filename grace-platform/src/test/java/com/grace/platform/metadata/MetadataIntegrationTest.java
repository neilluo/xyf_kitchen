package com.grace.platform.metadata;

import com.grace.platform.metadata.application.MetadataApplicationService;
import com.grace.platform.metadata.application.command.UpdateMetadataCommand;
import com.grace.platform.metadata.application.dto.VideoMetadataDTO;
import com.grace.platform.metadata.domain.VideoMetadata;
import com.grace.platform.metadata.domain.VideoMetadataRepository;
import com.grace.platform.metadata.infrastructure.llm.LlmRequest;
import com.grace.platform.metadata.infrastructure.llm.LlmResponse;
import com.grace.platform.metadata.infrastructure.llm.LlmService;
import com.grace.platform.GracePlatformApplication;
import com.grace.platform.shared.domain.id.MetadataId;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.testutil.AbstractIntegrationTest;
import com.grace.platform.testutil.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Metadata 上下文集成测试。
 * <p>
 * 测试元数据生成与确认流程：generate（Mock LLM）→ update → confirm。
 * 继承 AbstractIntegrationTest 使用 Testcontainers MySQL 8.0。
 * 使用 @MockBean 模拟 LlmService。
 * </p>
 */
@SpringBootTest(classes = GracePlatformApplication.class)
class MetadataIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MetadataApplicationService metadataService;

    @Autowired
    private VideoMetadataRepository metadataRepository;

    @MockBean
    private LlmService llmService;

    @BeforeEach
    void setupMocks() {
        // Mock LLM 返回预定义的元数据 JSON
        String mockResponse = """
            {
                "title": "美味红烧肉烹饪教程",
                "description": "详细讲解红烧肉的烹饪步骤，从选材到出锅全程演示",
                "tags": ["美食", "烹饪", "红烧肉", "家常菜", "教程"]
            }
            """;
        when(llmService.complete(any(LlmRequest.class)))
            .thenReturn(new LlmResponse(mockResponse, 100, 200));
    }

    @Test
    @DisplayName("元数据生成与确认完整流程")
    void metadataGenerationAndConfirmationFlow() {
        // Given: 一个视频 ID（模拟已有视频）
        VideoId videoId = TestFixtures.randomVideoId();

        // 1. Generate - 生成元数据（Mock LLM）
        VideoMetadataDTO generated = metadataService.generateMetadata(videoId);
        
        assertThat(generated).isNotNull();
        assertThat(generated.metadataId()).isNotBlank();
        assertThat(generated.videoId()).isEqualTo(videoId.value());
        assertThat(generated.title()).isEqualTo("美味红烧肉烹饪教程");
        assertThat(generated.description()).isEqualTo("详细讲解红烧肉的烹饪步骤，从选材到出锅全程演示");
        assertThat(generated.tags()).hasSize(5);
        assertThat(generated.confirmed()).isFalse();
        
        String metadataId = generated.metadataId();

        // 2. Update - 更新元数据
        UpdateMetadataCommand updateCommand = new UpdateMetadataCommand(
            "更新后的标题",
            "更新后的描述内容",
            List.of("更新", "标签", "列表", "美食", "教程")
        );
        
        VideoMetadataDTO updated = metadataService.updateMetadata(
            new MetadataId(metadataId), 
            updateCommand
        );
        
        assertThat(updated).isNotNull();
        assertThat(updated.title()).isEqualTo("更新后的标题");
        assertThat(updated.description()).isEqualTo("更新后的描述内容");
        assertThat(updated.tags()).hasSize(5);

        // 3. Confirm - 确认元数据
        VideoMetadataDTO confirmed = metadataService.confirmMetadata(new MetadataId(metadataId));
        
        assertThat(confirmed).isNotNull();
        assertThat(confirmed.confirmed()).isTrue();

        // 4. Verify - 验证持久化
        Optional<VideoMetadata> found = metadataRepository.findById(new MetadataId(metadataId));
        assertThat(found).isPresent();
        assertThat(found.get().getId().value()).isEqualTo(metadataId);
        assertThat(found.get().isConfirmed()).isTrue();
        assertThat(found.get().getTitle()).isEqualTo("更新后的标题");
    }

    @Test
    @DisplayName("元数据持久化往返：保存后应能通过 ID 正确查询")
    void metadataRoundTrip() {
        // Create and save metadata
        VideoId videoId = TestFixtures.randomVideoId();
        VideoMetadata metadata = TestFixtures.createMetadata(videoId);
        VideoMetadata saved = metadataRepository.save(metadata);
        
        // Query by ID
        Optional<VideoMetadata> found = metadataRepository.findById(saved.getId());
        
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getVideoId()).isEqualTo(saved.getVideoId());
        assertThat(found.get().getTitle()).isEqualTo(saved.getTitle());
        assertThat(found.get().getDescription()).isEqualTo(saved.getDescription());
        assertThat(found.get().getTags()).isEqualTo(saved.getTags());
    }

    @Test
    @DisplayName("通过视频 ID 查询最新元数据")
    void findLatestByVideoId() {
        // Given: 一个视频 ID
        VideoId videoId = TestFixtures.randomVideoId();
        
        // Create and save metadata
        VideoMetadata metadata = TestFixtures.createMetadata(videoId);
        metadataRepository.save(metadata);
        
        // Query by video ID
        VideoMetadataDTO found = metadataService.getMetadataByVideoId(videoId);
        
        assertThat(found).isNotNull();
        assertThat(found.videoId()).isEqualTo(videoId.value());
        assertThat(found.title()).isEqualTo("AI 生成标题");
    }
}
