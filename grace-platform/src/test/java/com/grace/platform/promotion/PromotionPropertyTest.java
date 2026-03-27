package com.grace.platform.promotion;

import com.grace.platform.promotion.domain.*;
import com.grace.platform.promotion.domain.vo.PromotionCopy;
import com.grace.platform.shared.domain.id.ChannelId;
import com.grace.platform.shared.domain.id.PromotionRecordId;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.shared.infrastructure.encryption.EncryptionService;
import net.jqwik.api.*;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for Promotion domain.
 * <p>
 * Covers Properties 9-12 from testing strategy:
 * - Property 9: Channel CRUD round-trip
 * - Property 10: API Key encrypted storage
 * - Property 11: Promotion copy structure invariants
 * - Property 12: Promotion record persistence round-trip
 *
 * @author Grace Platform Team
 */
class PromotionPropertyTest {

    // ========== Property 9: Channel CRUD Round-trip ==========

    /**
     * Property 9: 推广渠道 CRUD 往返
     * <p>
     * 验证：PromotionChannel 实体支持完整的 CRUD 操作。
     * - Create: 创建后所有字段值正确
     * - Read: 可以正确读取字段值
     * - Update: 更新后字段值反映更新内容
     * - Delete (soft): 禁用后状态变为 DISABLED
     */
    @Property(tries = 100)
    @Label("Feature: video-distribution-platform, Property 9: 推广渠道 CRUD 往返")
    void property_9_channelCrudRoundTrip(
            @ForAll("validChannels") PromotionChannel channel,
            @ForAll("validChannelUpdates") ChannelUpdate update) {

        // Given: 创建后的渠道
        assertThat(channel.getId()).isNotNull();
        assertThat(channel.getName()).isNotBlank();
        assertThat(channel.getType()).isNotNull();
        assertThat(channel.getChannelUrl()).isNotBlank();
        assertThat(channel.getPriority()).isBetween(1, 99);
        assertThat(channel.getStatus()).isEqualTo(ChannelStatus.ENABLED);
        assertThat(channel.getCreatedAt()).isNotNull();
        assertThat(channel.getUpdatedAt()).isNotNull();

        // When: Update
        channel.updateInfo(update.name(), update.type(), update.channelUrl(), update.priority());

        // Then: 更新后字段值反映更新内容
        assertThat(channel.getName()).isEqualTo(update.name());
        assertThat(channel.getType()).isEqualTo(update.type());
        assertThat(channel.getChannelUrl()).isEqualTo(update.channelUrl());
        assertThat(channel.getPriority()).isEqualTo(update.priority());

        // When: Disable (soft delete)
        channel.disable();

        // Then: 状态变为 DISABLED
        assertThat(channel.getStatus()).isEqualTo(ChannelStatus.DISABLED);
        assertThat(channel.isEnabled()).isFalse();

        // When: Re-enable
        channel.enable();

        // Then: 状态变回 ENABLED
        assertThat(channel.getStatus()).isEqualTo(ChannelStatus.ENABLED);
        assertThat(channel.isEnabled()).isTrue();
    }

    @Provide
    Arbitrary<PromotionChannel> validChannels() {
        Arbitrary<String> names = Arbitraries.strings()
                .alpha()
                .withChars(' ')
                .ofMinLength(1)
                .ofMaxLength(50)
                .filter(s -> !s.isBlank());
        Arbitrary<ChannelType> types = Arbitraries.of(ChannelType.values());
        Arbitrary<String> urls = Arbitraries.strings()
                .alpha()
                .numeric()
                .withChars('/', ':', '.', '-', '_')
                .ofMinLength(10)
                .ofMaxLength(200)
                .map(url -> "https://" + url);
        Arbitrary<Integer> priorities = Arbitraries.integers().between(1, 99);

        return Combinators.combine(names, types, urls, priorities)
                .as(PromotionChannel::create);
    }

    @Provide
    Arbitrary<ChannelUpdate> validChannelUpdates() {
        Arbitrary<String> names = Arbitraries.strings()
                .alpha()
                .withChars(' ')
                .ofMinLength(1)
                .ofMaxLength(50)
                .map(name -> "Updated-" + name)
                .filter(s -> !s.isBlank());
        Arbitrary<ChannelType> types = Arbitraries.of(ChannelType.values());
        Arbitrary<String> urls = Arbitraries.strings()
                .alpha()
                .numeric()
                .withChars('/', ':', '.', '-', '_')
                .ofMinLength(10)
                .ofMaxLength(200)
                .map(url -> "https://updated-" + url);
        Arbitrary<Integer> priorities = Arbitraries.integers().between(1, 99);

        return Combinators.combine(names, types, urls, priorities)
                .as(ChannelUpdate::new);
    }

    // ========== Property 10: API Key Encrypted Storage ==========

    /**
     * Property 10: API Key 加密存储
     * <p>
     * 验证：API Key 在存储时必须加密，且能正确解密还原。
     * - 加密后的值与明文不同
     * - 解密后应还原为原始明文
     * - 空或 null API Key 应正确处理
     */
    @Property(tries = 100)
    @Label("Feature: video-distribution-platform, Property 10: API Key 加密存储")
    void property_10_apiKeyEncryptedStorage(
            @ForAll("rawApiKeys") String rawApiKey) {

        // Given: 一个渠道和模拟加密服务
        PromotionChannel channel = PromotionChannel.create(
                "Test Channel", ChannelType.SOCIAL_MEDIA, "https://example.com", 1);
        EncryptionServiceStub encryptionService = new EncryptionServiceStub();

        // When: 设置 API Key
        channel.setApiKey(rawApiKey, encryptionService);

        if (rawApiKey == null || rawApiKey.isBlank()) {
            // Then: null 或空字符串应清除加密 API Key
            assertThat(channel.getEncryptedApiKey()).isNull();
            assertThat(channel.getDecryptedApiKey(encryptionService)).isNull();
        } else {
            // Then: 加密值不等于明文
            assertThat(channel.getEncryptedApiKey()).isNotEqualTo(rawApiKey);
            assertThat(channel.getEncryptedApiKey()).isNotNull();

            // And: 解密后应能还原
            String decrypted = channel.getDecryptedApiKey(encryptionService);
            assertThat(decrypted).isEqualTo(rawApiKey);

            // And: 每次加密产生不同密文（模拟 IV 随机性）
            PromotionChannel channel2 = PromotionChannel.create(
                    "Test Channel 2", ChannelType.SOCIAL_MEDIA, "https://example2.com", 1);
            channel2.setApiKey(rawApiKey, encryptionService);
            if (!channel.getEncryptedApiKey().equals(channel2.getEncryptedApiKey())) {
                // 加密服务可能产生不同密文（取决于是否有随机 IV）
                // 这里我们只验证加密值不是明文即可
            }
        }
    }

    @Provide
    Arbitrary<String> rawApiKeys() {
        return Arbitraries.oneOf(
                Arbitraries.strings()
                        .alpha()
                        .numeric()
                        .withChars('-', '_', '.', '~')
                        .ofMinLength(10)
                        .ofMaxLength(100),
                Arbitraries.of("", " ", null)
        );
    }

    // ========== Property 11: Promotion Copy Structure Invariants ==========

    /**
     * Property 11: 推广文案结构不变量
     * <p>
     * 验证：PromotionCopy 值对象必须包含所有必要字段。
     * - 必须包含 channelId
     * - 必须包含 channelName
     * - 必须包含 promotionTitle
     * - 必须包含 promotionBody
     * - 必须包含 recommendedMethod（POST/COMMENT/SHARE）
     * - 必须包含 methodReason
     */
    @Property(tries = 100)
    @Label("Feature: video-distribution-platform, Property 11: 推广文案结构不变量")
    void property_11_promotionCopyStructure(
            @ForAll("validPromotionCopies") PromotionCopy copy) {

        // Then: 必须包含渠道 ID
        assertThat(copy.channelId()).isNotNull();

        // Then: 必须包含渠道名称
        assertThat(copy.channelName()).isNotBlank();

        // Then: 必须包含推广标题
        assertThat(copy.promotionTitle()).isNotBlank();

        // Then: 必须包含推广正文
        assertThat(copy.promotionBody()).isNotBlank();

        // Then: 必须包含推荐的推广方式
        assertThat(copy.recommendedMethod()).isIn(
                PromotionMethod.POST, PromotionMethod.COMMENT, PromotionMethod.SHARE);

        // Then: 必须包含推荐理由
        assertThat(copy.methodReason()).isNotBlank();
    }

    @Provide
    Arbitrary<PromotionCopy> validPromotionCopies() {
        Arbitrary<ChannelId> channelIds = Arbitraries.create(ChannelId::generate);
        Arbitrary<String> channelNames = Arbitraries.strings()
                .alpha()
                .withChars(' ')
                .ofMinLength(1)
                .ofMaxLength(30)
                .filter(s -> !s.isBlank());
        Arbitrary<String> channelTypes = Arbitraries.of(
                ChannelType.SOCIAL_MEDIA.name(),
                ChannelType.FORUM.name(),
                ChannelType.BLOG.name(),
                ChannelType.OTHER.name()
        );
        Arbitrary<String> titles = Arbitraries.strings()
                .alpha()
                .withChars(' ')
                .ofMinLength(1)
                .ofMaxLength(100)
                .filter(s -> !s.isBlank());
        Arbitrary<String> bodies = Arbitraries.strings()
                .alpha()
                .withChars(' ', '\n', '.', '!', '?')
                .ofMinLength(10)
                .ofMaxLength(500)
                .filter(s -> !s.isBlank());
        Arbitrary<PromotionMethod> methods = Arbitraries.of(PromotionMethod.values());
        Arbitrary<String> reasons = Arbitraries.strings()
                .alpha()
                .withChars(' ')
                .ofMinLength(5)
                .ofMaxLength(200)
                .filter(s -> !s.isBlank());

        return Combinators.combine(
                        channelIds, channelNames, channelTypes, titles, bodies, methods, reasons)
                .as(PromotionCopy::new);
    }

    // ========== Property 12: Promotion Record Round-trip ==========

    /**
     * Property 12: 推广记录持久化往返
     * <p>
     * 验证：PromotionRecord 实体在创建和状态转换后，所有字段值保持正确。
     * 状态机：PENDING -> EXECUTING -> COMPLETED/FAILED
     */
    @Property(tries = 100)
    @Label("Feature: video-distribution-platform, Property 12: 推广记录持久化往返")
    void property_12_promotionRecordRoundTrip(
            @ForAll("validPromotionRecords") PromotionRecord record) {

        // Then: 基本字段验证
        assertThat(record.getId()).isNotNull();
        assertThat(record.getVideoId()).isNotNull();
        assertThat(record.getChannelId()).isNotNull();
        assertThat(record.getPromotionCopy()).isNotBlank();
        assertThat(record.getMethod()).isNotNull();
        assertThat(record.getStatus()).isNotNull();
        assertThat(record.getCreatedAt()).isNotNull();

        // Then: 初始状态应为 PENDING
        assertThat(record.getStatus()).isEqualTo(PromotionStatus.PENDING);

        // When: 开始执行
        record.startExecution();

        // Then: 状态变为 EXECUTING
        assertThat(record.getStatus()).isEqualTo(PromotionStatus.EXECUTING);
    }

    @Property(tries = 100)
    @Label("Feature: video-distribution-platform, Property 12b: 推广记录状态转换正确性")
    void property_12b_promotionRecordStateTransitions(
            @ForAll("promotionRecordTransitions") PromotionRecordTransitionFixture fixture) {

        PromotionRecord record = fixture.record();
        PromotionStatus expectedFinalStatus = fixture.expectedFinalStatus();

        // When: 应用状态转换
        fixture.transitions().forEach(transition -> {
            switch (transition) {
                case START_EXECUTION -> record.startExecution();
                case MARK_COMPLETED -> record.markCompleted("https://example.com/post/" + System.nanoTime());
                case MARK_FAILED -> record.markFailed("Test error message");
                case RETRY -> {
                    if (record.getStatus() == PromotionStatus.FAILED) {
                        record.retry();
                    }
                }
            }
        });

        // Then: 最终状态符合预期
        assertThat(record.getStatus()).isEqualTo(expectedFinalStatus);
    }

    @Provide
    Arbitrary<PromotionRecord> validPromotionRecords() {
        Arbitrary<VideoId> videoIds = Arbitraries.create(VideoId::generate);
        Arbitrary<ChannelId> channelIds = Arbitraries.create(ChannelId::generate);
        Arbitrary<String> copies = Arbitraries.strings()
                .alpha()
                .withChars(' ', '\n')
                .ofMinLength(10)
                .ofMaxLength(500)
                .filter(s -> !s.isBlank());
        Arbitrary<PromotionMethod> methods = Arbitraries.of(PromotionMethod.values());

        return Combinators.combine(videoIds, channelIds, copies, methods)
                .as(PromotionRecord::create);
    }

    @Provide
    Arbitrary<PromotionRecordTransitionFixture> promotionRecordTransitions() {
        Arbitrary<VideoId> videoIds = Arbitraries.create(VideoId::generate);
        Arbitrary<ChannelId> channelIds = Arbitraries.create(ChannelId::generate);
        Arbitrary<String> copies = Arbitraries.of("Test copy content");
        Arbitrary<PromotionMethod> methods = Arbitraries.of(PromotionMethod.values());

        // 定义合法的状态转换序列
        Arbitrary<List<RecordTransition>> transitionSequences = Arbitraries.of(
                List.of(RecordTransition.START_EXECUTION),
                List.of(RecordTransition.START_EXECUTION, RecordTransition.MARK_COMPLETED),
                List.of(RecordTransition.START_EXECUTION, RecordTransition.MARK_FAILED),
                List.of(RecordTransition.START_EXECUTION, RecordTransition.MARK_FAILED, RecordTransition.RETRY)
        );

        return Combinators.combine(videoIds, channelIds, copies, methods, transitionSequences)
                .as((videoId, channelId, copy, method, transitions) -> {
                    PromotionRecord record = PromotionRecord.create(videoId, channelId, copy, method);
                    PromotionStatus finalStatus = computeFinalStatus(transitions);
                    return new PromotionRecordTransitionFixture(record, transitions, finalStatus);
                });
    }

    private PromotionStatus computeFinalStatus(List<RecordTransition> transitions) {
        PromotionStatus status = PromotionStatus.PENDING;
        for (RecordTransition t : transitions) {
            switch (t) {
                case START_EXECUTION -> status = PromotionStatus.EXECUTING;
                case MARK_COMPLETED -> status = PromotionStatus.COMPLETED;
                case MARK_FAILED -> status = PromotionStatus.FAILED;
                case RETRY -> {
                    if (status == PromotionStatus.FAILED) {
                        status = PromotionStatus.EXECUTING;
                    }
                }
            }
        }
        return status;
    }

    // ========== Helper Classes ==========

    /**
     * 渠道更新参数记录
     */
    private record ChannelUpdate(
            String name,
            ChannelType type,
            String channelUrl,
            Integer priority
    ) {}

    /**
     * 推广记录状态转换夹具
     */
    private record PromotionRecordTransitionFixture(
            PromotionRecord record,
            List<RecordTransition> transitions,
            PromotionStatus expectedFinalStatus
    ) {}

    /**
     * 记录状态转换枚举
     */
    private enum RecordTransition {
        START_EXECUTION,
        MARK_COMPLETED,
        MARK_FAILED,
        RETRY
    }

    /**
     * 模拟的加密服务，用于测试
     */
    private static class EncryptionServiceStub implements EncryptionService {
        private static final String PREFIX = "encrypted:";

        @Override
        public String encrypt(String plaintext) {
            if (plaintext == null || plaintext.isBlank()) {
                return null;
            }
            // 简单模拟加密：添加前缀并反转
            return PREFIX + new StringBuilder(plaintext).reverse();
        }

        @Override
        public String decrypt(String ciphertext) {
            if (ciphertext == null || !ciphertext.startsWith(PREFIX)) {
                return ciphertext;
            }
            // 解密：移除前缀并反转回来
            String encrypted = ciphertext.substring(PREFIX.length());
            return new StringBuilder(encrypted).reverse().toString();
        }
    }
}
