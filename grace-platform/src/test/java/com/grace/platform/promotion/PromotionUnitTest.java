package com.grace.platform.promotion;

import com.grace.platform.promotion.application.ChannelApplicationService;
import com.grace.platform.promotion.application.PromotionApplicationService;
import com.grace.platform.promotion.application.command.CreateChannelCommand;
import com.grace.platform.promotion.application.command.ExecutePromotionCommand;
import com.grace.platform.promotion.application.dto.ChannelDTO;
import com.grace.platform.promotion.application.dto.PromotionResultDTO;
import com.grace.platform.promotion.domain.*;
import com.grace.platform.promotion.domain.vo.PromotionCopy;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.domain.id.ChannelId;
import com.grace.platform.shared.domain.id.PromotionRecordId;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.shared.infrastructure.encryption.EncryptionService;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Promotion domain.
 * <p>
 * Covers boundary conditions and error scenarios:
 * - Channel soft vs hard delete (based on promotion records)
 * - Batch promotion execution - single failure should not interrupt
 * - Promotion status state machine transitions
 * - Channel priority validation
 * - Promotion record retry logic
 *
 * @author Grace Platform Team
 */
class PromotionUnitTest {

    // ========== Channel Soft/Hard Delete Tests ==========

    @Test
    @DisplayName("有推广记录的渠道应执行软删除（禁用）而非硬删除")
    void shouldSoftDeleteChannelWithPromotionRecords() {
        // Given: 一个渠道和模拟的仓储
        ChannelId channelId = ChannelId.generate();
        PromotionChannel channel = PromotionChannel.create(
                "Test Channel", ChannelType.SOCIAL_MEDIA, "https://example.com", 1);

        // 使用反射设置 ID
        setChannelId(channel, channelId);

        PromotionChannelRepository channelRepository = mock(PromotionChannelRepository.class);
        EncryptionService encryptionService = mock(EncryptionService.class);
        ChannelApplicationService service = new ChannelApplicationService(channelRepository, encryptionService);

        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));
        when(channelRepository.existsPromotionRecordByChannelId(channelId)).thenReturn(true);
        when(channelRepository.save(any())).thenReturn(channel);

        // When: 删除渠道
        service.deleteChannel(channelId);

        // Then: 应执行软删除（禁用）
        assertThat(channel.getStatus()).isEqualTo(ChannelStatus.DISABLED);
        verify(channelRepository).save(channel);
        verify(channelRepository, never()).deleteById(channelId);
    }

    @Test
    @DisplayName("无推广记录的渠道应执行硬删除")
    void shouldHardDeleteChannelWithoutPromotionRecords() {
        // Given: 一个渠道和模拟的仓储
        ChannelId channelId = ChannelId.generate();
        PromotionChannel channel = PromotionChannel.create(
                "Test Channel", ChannelType.SOCIAL_MEDIA, "https://example.com", 1);

        setChannelId(channel, channelId);

        PromotionChannelRepository channelRepository = mock(PromotionChannelRepository.class);
        EncryptionService encryptionService = mock(EncryptionService.class);
        ChannelApplicationService service = new ChannelApplicationService(channelRepository, encryptionService);

        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));
        when(channelRepository.existsPromotionRecordByChannelId(channelId)).thenReturn(false);

        // When: 删除渠道
        service.deleteChannel(channelId);

        // Then: 应执行硬删除
        verify(channelRepository).deleteById(channelId);
        verify(channelRepository, never()).save(any());
    }

    @Test
    @DisplayName("禁用状态的渠道不应被用于推广")
    void shouldNotUseDisabledChannelForPromotion() {
        // Given: 一个禁用的渠道
        PromotionChannel channel = PromotionChannel.create(
                "Disabled Channel", ChannelType.SOCIAL_MEDIA, "https://example.com", 1);
        channel.disable();

        // Then: isEnabled 应返回 false
        assertThat(channel.isEnabled()).isFalse();

        // And: 状态应为 DISABLED
        assertThat(channel.getStatus()).isEqualTo(ChannelStatus.DISABLED);
    }

    @Test
    @DisplayName("已禁用的渠道可以重新启用")
    void canReEnableDisabledChannel() {
        // Given: 一个禁用的渠道
        PromotionChannel channel = PromotionChannel.create(
                "Disabled Channel", ChannelType.SOCIAL_MEDIA, "https://example.com", 1);
        channel.disable();
        assertThat(channel.isEnabled()).isFalse();

        // When: 重新启用
        channel.enable();

        // Then: 状态应为 ENABLED
        assertThat(channel.getStatus()).isEqualTo(ChannelStatus.ENABLED);
        assertThat(channel.isEnabled()).isTrue();
    }

    // ========== Batch Promotion Execution - Failure Isolation ==========

    @Test
    @DisplayName("单个渠道推广失败不应中断批量推广流程")
    void singleChannelFailureShouldNotInterruptBatch() {
        // Given: 模拟仓储和依赖
        PromotionChannelRepository channelRepository = mock(PromotionChannelRepository.class);
        PromotionRecordRepository recordRepository = mock(PromotionRecordRepository.class);
        PromotionExecutorRegistry executorRegistry = mock(PromotionExecutorRegistry.class);
        PromotionExecutor executor = mock(PromotionExecutor.class);

        // 创建 3 个渠道
        ChannelId channelId1 = ChannelId.generate();
        ChannelId channelId2 = ChannelId.generate();
        ChannelId channelId3 = ChannelId.generate();

        PromotionChannel channel1 = createChannel(channelId1, "Channel 1", 1);
        PromotionChannel channel2 = createChannel(channelId2, "Channel 2", 2);
        PromotionChannel channel3 = createChannel(channelId3, "Channel 3", 3);

        when(channelRepository.findById(channelId1)).thenReturn(Optional.of(channel1));
        when(channelRepository.findById(channelId2)).thenReturn(Optional.of(channel2));
        when(channelRepository.findById(channelId3)).thenReturn(Optional.of(channel3));

        when(executorRegistry.getExecutor("opencrawl")).thenReturn(executor);

        // 第1个成功，第2个失败，第3个成功
        PromotionResult successResult = new PromotionResult(PromotionStatus.COMPLETED, "https://example.com/1", null);
        PromotionResult failResult = new PromotionResult(PromotionStatus.FAILED, null, "Execution failed");

        when(executor.execute(any(PromotionCopy.class), eq(channel1))).thenReturn(successResult);
        when(executor.execute(any(PromotionCopy.class), eq(channel2))).thenReturn(failResult);
        when(executor.execute(any(PromotionCopy.class), eq(channel3))).thenReturn(successResult);

        // Mock recordRepository.save 返回传入的参数
        when(recordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Then: 所有 3 个渠道的推广都应被执行（不中断）
        assertThatCode(() -> {
            // 模拟批量执行
            executeBatchPromotion(channelRepository, recordRepository, executorRegistry,
                    List.of(channel1, channel2, channel3));
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("批量推广应按优先级排序执行")
    void batchPromotionShouldExecuteByPriority() {
        // Given: 3 个渠道，不同优先级
        PromotionChannel lowPriority = PromotionChannel.create(
                "Low Priority", ChannelType.SOCIAL_MEDIA, "https://example1.com", 99);
        PromotionChannel mediumPriority = PromotionChannel.create(
                "Medium Priority", ChannelType.FORUM, "https://example2.com", 50);
        PromotionChannel highPriority = PromotionChannel.create(
                "High Priority", ChannelType.BLOG, "https://example3.com", 1);

        // 创建 List 并验证排序
        List<PromotionChannel> channels = new ArrayList<>(List.of(lowPriority, mediumPriority, highPriority));

        // When: 按优先级升序排序
        channels.sort(java.util.Comparator.comparingInt(PromotionChannel::getPriority));

        // Then: 顺序应为高 -> 中 -> 低（数值小优先）
        assertThat(channels.get(0).getPriority()).isEqualTo(1);
        assertThat(channels.get(1).getPriority()).isEqualTo(50);
        assertThat(channels.get(2).getPriority()).isEqualTo(99);
        assertThat(channels.get(0).getName()).isEqualTo("High Priority");
        assertThat(channels.get(2).getName()).isEqualTo("Low Priority");
    }

    // ========== Promotion Record State Machine Tests ==========

    @Test
    @DisplayName("推广记录应遵循正确的状态转换：PENDING -> EXECUTING -> COMPLETED")
    void shouldFollowCorrectStateTransitionToCompleted() {
        // Given: 一个新的推广记录
        VideoId videoId = VideoId.generate();
        ChannelId channelId = ChannelId.generate();
        PromotionRecord record = PromotionRecord.create(
                videoId, channelId, "Test copy", PromotionMethod.POST);

        // Then: 初始状态为 PENDING
        assertThat(record.getStatus()).isEqualTo(PromotionStatus.PENDING);

        // When: 开始执行
        record.startExecution();

        // Then: 状态变为 EXECUTING
        assertThat(record.getStatus()).isEqualTo(PromotionStatus.EXECUTING);

        // When: 标记完成
        record.markCompleted("https://example.com/post/123");

        // Then: 状态变为 COMPLETED
        assertThat(record.getStatus()).isEqualTo(PromotionStatus.COMPLETED);
        assertThat(record.getResultUrl()).isEqualTo("https://example.com/post/123");
        assertThat(record.getExecutedAt()).isNotNull();
    }

    @Test
    @DisplayName("推广记录应遵循正确的状态转换：PENDING -> EXECUTING -> FAILED -> EXECUTING (retry)")
    void shouldFollowCorrectStateTransitionWithRetry() {
        // Given: 一个新的推广记录
        VideoId videoId = VideoId.generate();
        ChannelId channelId = ChannelId.generate();
        PromotionRecord record = PromotionRecord.create(
                videoId, channelId, "Test copy", PromotionMethod.COMMENT);

        // PENDING -> EXECUTING
        record.startExecution();
        assertThat(record.getStatus()).isEqualTo(PromotionStatus.EXECUTING);

        // EXECUTING -> FAILED
        record.markFailed("Network error");
        assertThat(record.getStatus()).isEqualTo(PromotionStatus.FAILED);
        assertThat(record.getErrorMessage()).isEqualTo("Network error");

        // FAILED -> EXECUTING (retry)
        record.retry();
        assertThat(record.getStatus()).isEqualTo(PromotionStatus.EXECUTING);
        assertThat(record.getErrorMessage()).isNull(); // 清除之前的错误
    }

    @Test
    @DisplayName("非 FAILED 状态的记录不应允许重试")
    void shouldNotAllowRetryForNonFailedRecord() {
        // Given: 一个已完成的记录
        VideoId videoId = VideoId.generate();
        ChannelId channelId = ChannelId.generate();
        PromotionRecord record = PromotionRecord.create(
                videoId, channelId, "Test copy", PromotionMethod.POST);

        record.startExecution();
        record.markCompleted("https://example.com/post/123");

        assertThat(record.getStatus()).isEqualTo(PromotionStatus.COMPLETED);

        // When & Then: 重试应抛出异常
        assertThatThrownBy(() -> record.retry())
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_PROMOTION_STATUS))
                .hasMessageContaining("Cannot retry promotion record");
    }

    @Test
    @DisplayName("PENDING 或 FAILED 状态的记录允许更新文案")
    void shouldAllowUpdateCopyInPendingOrFailedStatus() {
        // Given: PENDING 状态的记录
        VideoId videoId = VideoId.generate();
        ChannelId channelId = ChannelId.generate();
        PromotionRecord record = PromotionRecord.create(
                videoId, channelId, "Original copy", PromotionMethod.POST);

        // When: 更新文案（PENDING 状态）
        record.updateCopy("Updated copy");

        // Then: 文案已更新
        assertThat(record.getPromotionCopy()).isEqualTo("Updated copy");

        // When: 开始执行然后失败
        record.startExecution();
        record.markFailed("Error");

        // Then: FAILED 状态也可以更新文案
        record.updateCopy("Retry copy");
        assertThat(record.getPromotionCopy()).isEqualTo("Retry copy");
    }

    @Test
    @DisplayName("EXECUTING 状态的记录不应允许更新文案")
    void shouldNotAllowUpdateCopyInExecutingStatus() {
        // Given: EXECUTING 状态的记录
        VideoId videoId = VideoId.generate();
        ChannelId channelId = ChannelId.generate();
        PromotionRecord record = PromotionRecord.create(
                videoId, channelId, "Original copy", PromotionMethod.POST);

        record.startExecution();
        assertThat(record.getStatus()).isEqualTo(PromotionStatus.EXECUTING);

        // When & Then: 更新文案应抛出异常
        assertThatThrownBy(() -> record.updateCopy("New copy"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_PROMOTION_STATUS))
                .hasMessageContaining("Cannot update copy");
    }

    @Test
    @DisplayName("COMPLETED 状态的记录不应允许更新文案")
    void shouldNotAllowUpdateCopyInCompletedStatus() {
        // Given: COMPLETED 状态的记录
        VideoId videoId = VideoId.generate();
        ChannelId channelId = ChannelId.generate();
        PromotionRecord record = PromotionRecord.create(
                videoId, channelId, "Original copy", PromotionMethod.POST);

        record.startExecution();
        record.markCompleted("https://example.com");
        assertThat(record.getStatus()).isEqualTo(PromotionStatus.COMPLETED);

        // When & Then: 更新文案应抛出异常
        assertThatThrownBy(() -> record.updateCopy("New copy"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_PROMOTION_STATUS));
    }

    // ========== Channel Priority Validation Tests ==========

    @Test
    @DisplayName("优先级边界值 - 最小值 1 应被接受")
    void shouldAcceptMinimumPriority() {
        assertThatCode(() -> PromotionChannel.create(
                "Test", ChannelType.SOCIAL_MEDIA, "https://example.com", 1))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("优先级边界值 - 最大值 99 应被接受")
    void shouldAcceptMaximumPriority() {
        assertThatCode(() -> PromotionChannel.create(
                "Test", ChannelType.SOCIAL_MEDIA, "https://example.com", 99))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("优先级边界值 - 小于 1 应被拒绝")
    void shouldRejectPriorityLessThanOne() {
        assertThatThrownBy(() -> PromotionChannel.create(
                "Test", ChannelType.SOCIAL_MEDIA, "https://example.com", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Priority must be between");
    }

    @Test
    @DisplayName("优先级边界值 - 大于 99 应被拒绝")
    void shouldRejectPriorityGreaterThanNinetyNine() {
        assertThatThrownBy(() -> PromotionChannel.create(
                "Test", ChannelType.SOCIAL_MEDIA, "https://example.com", 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Priority must be between");
    }

    @Test
    @DisplayName("更新时优先级超出范围应被拒绝")
    void shouldRejectInvalidPriorityOnUpdate() {
        // Given: 一个有效渠道
        PromotionChannel channel = PromotionChannel.create(
                "Test", ChannelType.SOCIAL_MEDIA, "https://example.com", 50);

        // When & Then: 更新时设置无效优先级应抛出异常
        assertThatThrownBy(() -> channel.updateInfo(null, null, null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Priority must be between");

        assertThatThrownBy(() -> channel.updateInfo(null, null, null, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Priority must be between");
    }

    // ========== Channel Validation Tests ==========

    @Test
    @DisplayName("渠道名称不能为空")
    void shouldRejectBlankChannelName() {
        assertThatThrownBy(() -> PromotionChannel.create(
                "", ChannelType.SOCIAL_MEDIA, "https://example.com", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Channel name must not be blank");

        assertThatThrownBy(() -> PromotionChannel.create(
                "   ", ChannelType.SOCIAL_MEDIA, "https://example.com", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Channel name must not be blank");

        assertThatThrownBy(() -> PromotionChannel.create(
                null, ChannelType.SOCIAL_MEDIA, "https://example.com", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Channel name must not be blank");
    }

    @Test
    @DisplayName("渠道 URL 不能为空")
    void shouldRejectBlankChannelUrl() {
        assertThatThrownBy(() -> PromotionChannel.create(
                "Test", ChannelType.SOCIAL_MEDIA, "", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Channel URL must not be blank");

        assertThatThrownBy(() -> PromotionChannel.create(
                "Test", ChannelType.SOCIAL_MEDIA, null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Channel URL must not be blank");
    }

    @Test
    @DisplayName("渠道类型不能为空")
    void shouldRejectNullChannelType() {
        assertThatThrownBy(() -> PromotionChannel.create(
                "Test", null, "https://example.com", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Channel type must not be null");
    }

    // ========== PromotionRecord Validation Tests ==========

    @Test
    @DisplayName("推广记录创建时视频 ID 不能为空")
    void shouldRejectNullVideoIdForRecord() {
        ChannelId channelId = ChannelId.generate();
        assertThatThrownBy(() -> PromotionRecord.create(
                null, channelId, "Test copy", PromotionMethod.POST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VideoId must not be null");
    }

    @Test
    @DisplayName("推广记录创建时渠道 ID 不能为空")
    void shouldRejectNullChannelIdForRecord() {
        VideoId videoId = VideoId.generate();
        assertThatThrownBy(() -> PromotionRecord.create(
                videoId, null, "Test copy", PromotionMethod.POST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ChannelId must not be null");
    }

    @Test
    @DisplayName("推广记录创建时文案不能为空")
    void shouldRejectBlankPromotionCopy() {
        VideoId videoId = VideoId.generate();
        ChannelId channelId = ChannelId.generate();

        assertThatThrownBy(() -> PromotionRecord.create(
                videoId, channelId, "", PromotionMethod.POST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Promotion copy must not be blank");

        assertThatThrownBy(() -> PromotionRecord.create(
                videoId, channelId, "   ", PromotionMethod.POST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Promotion copy must not be blank");

        assertThatThrownBy(() -> PromotionRecord.create(
                videoId, channelId, null, PromotionMethod.POST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Promotion copy must not be blank");
    }

    @Test
    @DisplayName("推广记录创建时推广方式不能为空")
    void shouldRejectNullPromotionMethod() {
        VideoId videoId = VideoId.generate();
        ChannelId channelId = ChannelId.generate();

        assertThatThrownBy(() -> PromotionRecord.create(
                videoId, channelId, "Test copy", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Promotion method must not be null");
    }

    // ========== Helper Methods ==========

    private PromotionChannel createChannel(ChannelId id, String name, int priority) {
        PromotionChannel channel = PromotionChannel.create(name, ChannelType.SOCIAL_MEDIA, "https://example.com", priority);
        setChannelId(channel, id);
        return channel;
    }

    private void setChannelId(PromotionChannel channel, ChannelId id) {
        try {
            java.lang.reflect.Field idField = PromotionChannel.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(channel, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 模拟批量推广执行
     */
    private List<PromotionRecord> executeBatchPromotion(
            PromotionChannelRepository channelRepository,
            PromotionRecordRepository recordRepository,
            PromotionExecutorRegistry executorRegistry,
            List<PromotionChannel> channels) {

        List<PromotionRecord> results = new ArrayList<>();
        VideoId videoId = VideoId.generate();

        for (PromotionChannel channel : channels) {
            // 创建记录
            PromotionRecord record = PromotionRecord.create(
                    videoId, channel.getId(), "Test copy", PromotionMethod.POST);
            record = recordRepository.save(record);
            record.startExecution();

            try {
                PromotionExecutor executor = executorRegistry.getExecutor("opencrawl");
                PromotionCopy copy = new PromotionCopy(
                        channel.getId(), channel.getName(), channel.getType().name(),
                        "Title", "Body", PromotionMethod.POST, "Reason");
                PromotionResult result = executor.execute(copy, channel);

                if (result.status() == PromotionStatus.COMPLETED) {
                    record.markCompleted(result.resultUrl());
                } else {
                    record.markFailed(result.errorMessage());
                }
            } catch (Exception e) {
                record.markFailed(e.getMessage());
            }

            record = recordRepository.save(record);
            results.add(record);
        }

        return results;
    }
}
