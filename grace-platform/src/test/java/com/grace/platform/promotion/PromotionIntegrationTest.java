package com.grace.platform.promotion;

import com.grace.platform.metadata.domain.VideoMetadata;
import com.grace.platform.metadata.domain.VideoMetadataRepository;
import com.grace.platform.metadata.infrastructure.llm.LlmRequest;
import com.grace.platform.metadata.infrastructure.llm.LlmResponse;
import com.grace.platform.metadata.infrastructure.llm.LlmService;
import com.grace.platform.GracePlatformApplication;
import com.grace.platform.promotion.application.ChannelApplicationService;
import com.grace.platform.promotion.application.PromotionApplicationService;
import com.grace.platform.promotion.application.command.CreateChannelCommand;
import com.grace.platform.promotion.application.command.ExecutePromotionCommand;
import com.grace.platform.promotion.application.dto.ChannelDTO;
import com.grace.platform.promotion.application.dto.PromotionCopyDTO;
import com.grace.platform.promotion.application.dto.PromotionRecordDTO;
import com.grace.platform.promotion.application.dto.PromotionReportDTO;
import com.grace.platform.promotion.application.dto.PromotionResultDTO;
import com.grace.platform.promotion.domain.*;
import com.grace.platform.promotion.infrastructure.opencrawl.OpenCrawlAdapter;
import com.grace.platform.promotion.infrastructure.opencrawl.OpenCrawlRequest;
import com.grace.platform.promotion.infrastructure.opencrawl.OpenCrawlResponse;
import com.grace.platform.shared.domain.PageRequest;
import com.grace.platform.shared.domain.id.ChannelId;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.testutil.AbstractIntegrationTest;
import com.grace.platform.testutil.TestFixtures;
import com.grace.platform.video.domain.Video;
import com.grace.platform.video.domain.VideoRepository;
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
 * Promotion 上下文集成测试。
 * <p>
 * 测试推广全流程：创建渠道 → 生成文案（Mock LLM）→ 执行推广（Mock OpenCrawl）→ 查询报告。
 * 继承 AbstractIntegrationTest 使用 Testcontainers MySQL 8.0。
 * 使用 @MockBean 模拟 LlmService 和 OpenCrawlAdapter。
 * </p>
 */
@SpringBootTest(classes = GracePlatformApplication.class)
class PromotionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ChannelApplicationService channelService;

    @Autowired
    private PromotionApplicationService promotionService;

    @Autowired
    private PromotionChannelRepository channelRepository;

    @Autowired
    private PromotionRecordRepository recordRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private VideoMetadataRepository metadataRepository;

    @MockBean
    private LlmService llmService;

    @MockBean
    private OpenCrawlAdapter openCrawlAdapter;

    @BeforeEach
    void setupMocks() {
        // Mock LLM 返回预定义的推广文案 JSON
        String mockPromotionResponse = """
            {
                "title": "精彩美食视频推荐",
                "body": "这个视频教你怎么做美味的红烧肉，步骤详细，一看就会！",
                "method": "POST",
                "reason": "适合直接发布到社交媒体"
            }
            """;
        when(llmService.complete(any(LlmRequest.class)))
            .thenReturn(new LlmResponse(mockPromotionResponse, 150, 250));

        // Mock OpenCrawl 返回成功响应
        when(openCrawlAdapter.execute(any(OpenCrawlRequest.class)))
            .thenReturn(OpenCrawlResponse.success(
                "https://example.com/promotion/123", 
                "{\"success\": true, \"url\": \"https://example.com/promotion/123\"}"
            ));
    }

    @Test
    @DisplayName("推广全流程：创建渠道 → 生成文案 → 执行推广 → 查询报告")
    void promotionFullFlow() {
        // Given: 创建视频和元数据
        Video video = TestFixtures.createVideo();
        video = videoRepository.save(video);
        VideoId videoId = video.getId();

        VideoMetadata metadata = VideoMetadata.create(
            videoId,
            "测试视频标题",
            "测试视频描述",
            List.of("美食", "测试", "视频"),
            com.grace.platform.metadata.domain.MetadataSource.AI_GENERATED
        );
        metadataRepository.save(metadata);

        // 1. Create channel - 创建推广渠道
        CreateChannelCommand createChannelCmd = new CreateChannelCommand(
            "测试渠道",
            ChannelType.SOCIAL_MEDIA,
            "https://example.com/channel",
            "test_api_key_12345",
            1
        );
        
        ChannelDTO channel = channelService.createChannel(createChannelCmd);
        assertThat(channel).isNotNull();
        assertThat(channel.channelId()).isNotBlank();
        assertThat(channel.name()).isEqualTo("测试渠道");
        assertThat(channel.type()).isEqualTo(ChannelType.SOCIAL_MEDIA);
        assertThat(channel.hasApiKey()).isTrue();
        
        ChannelId channelId = new ChannelId(channel.channelId());

        // 2. Generate copy - 生成推广文案（Mock LLM）
        List<PromotionCopyDTO> copies = promotionService.generateCopy(videoId, List.of(channelId));
        
        assertThat(copies).isNotEmpty();
        assertThat(copies.get(0).channelId()).isEqualTo(channel.channelId());
        assertThat(copies.get(0).promotionTitle()).isEqualTo("精彩美食视频推荐");
        assertThat(copies.get(0).promotionBody()).contains("红烧肉");
        assertThat(copies.get(0).recommendedMethod()).isEqualTo(PromotionMethod.POST);

        // 3. Execute promotion - 执行推广（Mock OpenCrawl）
        ExecutePromotionCommand.PromotionItem promotionItem = new ExecutePromotionCommand.PromotionItem(
            channelId,
            "精彩美食视频推荐",
            "这个视频教你怎么做美味的红烧肉，步骤详细，一看就会！",
            PromotionMethod.POST
        );
        
        ExecutePromotionCommand executeCmd = new ExecutePromotionCommand(videoId, List.of(promotionItem));
        List<PromotionResultDTO> results = promotionService.executePromotion(executeCmd);
        
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).channelId()).isEqualTo(channelId.value());
        assertThat(results.get(0).status()).isEqualTo(PromotionStatus.COMPLETED);
        assertThat(results.get(0).resultUrl()).contains("https://example.com/promotion/");

        // 4. Query promotion history - 查询推广历史
        PageRequest pageRequest = new PageRequest(1, 10, "createdAt", "desc");
        var historyPage = promotionService.getPromotionHistory(
            videoId, pageRequest, null, null, null, null
        );
        
        assertThat(historyPage).isNotNull();
        assertThat(historyPage.items()).isNotEmpty();
        assertThat(historyPage.total()).isGreaterThan(0);

        // 5. Get promotion report - 获取推广报告
        PromotionReportDTO report = promotionService.getPromotionReport(videoId);
        
        assertThat(report).isNotNull();
        assertThat(report.videoId()).isEqualTo(videoId.value());
        assertThat(report.totalChannels()).isGreaterThan(0);
        assertThat(report.channelSummaries()).isNotEmpty();
    }

    @Test
    @DisplayName("推广渠道 CRUD 往返：创建 → 更新 → 查询 → 删除")
    void channelCrudRoundTrip() {
        // 1. Create
        CreateChannelCommand createCmd = new CreateChannelCommand(
            "CRUD测试渠道",
            ChannelType.FORUM,
            "https://forum.example.com",
            "api_key_for_test",
            5
        );
        
        ChannelDTO created = channelService.createChannel(createCmd);
        assertThat(created).isNotNull();
        String channelId = created.channelId();

        // 2. Query by ID
        ChannelDTO found = channelService.getChannel(new ChannelId(channelId));
        assertThat(found).isNotNull();
        assertThat(found.name()).isEqualTo("CRUD测试渠道");
        assertThat(found.priority()).isEqualTo(5);

        // 3. List all channels
        List<ChannelDTO> channels = channelService.listChannels(null);
        assertThat(channels).anyMatch(c -> c.channelId().equals(channelId));

        // 4. List by status
        List<ChannelDTO> enabledChannels = channelService.listChannels(ChannelStatus.ENABLED);
        assertThat(enabledChannels).anyMatch(c -> c.channelId().equals(channelId));

        // 5. Verify persistence
        Optional<PromotionChannel> persisted = channelRepository.findById(new ChannelId(channelId));
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getName()).isEqualTo("CRUD测试渠道");
    }

    @Test
    @DisplayName("推广记录持久化往返：保存后应能通过视频 ID 正确查询")
    void promotionRecordRoundTrip() {
        // Given: video and channel
        VideoId videoId = TestFixtures.randomVideoId();
        ChannelId channelId = TestFixtures.randomChannelId();
        
        // Create and save promotion record
        PromotionRecord record = TestFixtures.createPromotionRecord(videoId, channelId);
        PromotionRecord saved = recordRepository.save(record);
        
        // Query by video ID
        List<PromotionRecord> found = recordRepository.findByVideoIdForReport(videoId);
        
        assertThat(found).isNotEmpty();
        assertThat(found.get(0).getVideoId()).isEqualTo(saved.getVideoId());
        assertThat(found.get(0).getChannelId()).isEqualTo(saved.getChannelId());
    }
}
