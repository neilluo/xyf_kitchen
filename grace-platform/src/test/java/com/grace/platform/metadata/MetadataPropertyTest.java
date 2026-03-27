package com.grace.platform.metadata;

import com.grace.platform.metadata.domain.MetadataSource;
import com.grace.platform.metadata.domain.VideoMetadata;
import com.grace.platform.shared.domain.id.VideoId;
import net.jqwik.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for Metadata domain.
 * <p>
 * Covers Properties 4-5 from testing strategy:
 * - Property 4: Metadata field constraint invariants (title≤100, description≤5000, tags 5-15)
 * - Property 5: Metadata edit round-trip (update method)
 * <p>
 * Note: Persistence round-trip tests are covered in MetadataIntegrationTest
 * as they require Spring context and database access.
 *
 * @author Grace Platform Team
 */
class MetadataPropertyTest {

    // ========== Property 4: Metadata Field Constraint Invariants ==========

    /**
     * Property 4: 元数据字段约束不变量
     * <p>
     * 验证：通过 create() 方法创建的 VideoMetadata 实体必须满足以下约束：
     * - 标题非空且不超过100字符
     * - 描述不超过5000字符
     * - 标签数量在5-15之间
     */
    @Property(tries = 100)
    @Label("Feature: video-distribution-platform, Property 4: 元数据字段约束不变量")
    void property_4_metadataConstraints(
            @ForAll("validMetadataCreationParams") MetadataCreationParams params) {
        // When: 创建 VideoMetadata 实体
        VideoMetadata metadata = VideoMetadata.create(
                params.videoId(),
                params.title(),
                params.description(),
                params.tags(),
                params.source()
        );

        // Then: 以下约束必须同时成立
        assertThat(metadata.getTitle()).isNotBlank();
        assertThat(metadata.getTitle().length()).isLessThanOrEqualTo(100);
        if (metadata.getDescription() != null) {
            assertThat(metadata.getDescription().length()).isLessThanOrEqualTo(5000);
        }
        assertThat(metadata.getTags()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(metadata.getTags()).hasSizeLessThanOrEqualTo(15);
        assertThat(metadata.isConfirmed()).isFalse();
    }

    @Provide
    Arbitrary<MetadataCreationParams> validMetadataCreationParams() {
        Arbitrary<VideoId> videoIds = Arbitraries.create(VideoId::generate);
        Arbitrary<String> titles = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('\u4e00', '\u9fff')  // 支持中文
                .ofMinLength(1)
                .ofMaxLength(100);
        Arbitrary<String> descriptions = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('\u4e00', '\u9fff')
                .ofMinLength(0)
                .ofMaxLength(5000);
        Arbitrary<List<String>> tags = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20)
                .list()
                .ofMinSize(5)
                .ofMaxSize(15);
        Arbitrary<MetadataSource> sources = Arbitraries.of(MetadataSource.values());

        return Combinators.combine(videoIds, titles, descriptions, tags, sources)
                .as(MetadataCreationParams::new);
    }

    // ========== Property 5: Metadata Edit Round-trip ==========

    /**
     * Property 5: 元数据编辑往返
     * <p>
     * 验证：对未确认的元数据执行 update() 操作后，
     * 所有字段更新应正确反映，source 应变为 AI_EDITED（如果原来是 AI_GENERATED）。
     */
    @Property(tries = 100)
    @Label("Feature: video-distribution-platform, Property 5: 元数据编辑往返")
    void property_5_metadataEditRoundTrip(
            @ForAll("validMetadataCreationParams") MetadataCreationParams originalParams,
            @ForAll("validMetadataUpdateParams") MetadataUpdateParams updateParams) {
        // Given: 创建一个未确认的元数据
        VideoMetadata metadata = VideoMetadata.create(
                originalParams.videoId(),
                originalParams.title(),
                originalParams.description(),
                originalParams.tags(),
                originalParams.source()
        );

        // When: 执行更新
        metadata.update(updateParams.title(), updateParams.description(), updateParams.tags());

        // Then: 更新后查询应反映所有更新内容
        if (updateParams.title() != null) {
            assertThat(metadata.getTitle()).isEqualTo(updateParams.title());
        } else {
            assertThat(metadata.getTitle()).isEqualTo(originalParams.title());
        }

        if (updateParams.description() != null) {
            assertThat(metadata.getDescription()).isEqualTo(updateParams.description());
        } else {
            assertThat(metadata.getDescription()).isEqualTo(originalParams.description());
        }

        if (updateParams.tags() != null) {
            assertThat(metadata.getTags()).isEqualTo(updateParams.tags());
        } else {
            assertThat(metadata.getTags()).isEqualTo(originalParams.tags());
        }

        // And: source 应变为 AI_EDITED（如果原来是 AI_GENERATED）
        if (originalParams.source() == MetadataSource.AI_GENERATED) {
            assertThat(metadata.getSource()).isEqualTo(MetadataSource.AI_EDITED);
        } else {
            assertThat(metadata.getSource()).isEqualTo(originalParams.source());
        }
    }

    @Provide
    Arbitrary<MetadataUpdateParams> validMetadataUpdateParams() {
        Arbitrary<String> titles = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('\u4e00', '\u9fff')
                .ofMinLength(1)
                .ofMaxLength(100);
        Arbitrary<String> descriptions = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('\u4e00', '\u9fff')
                .ofMinLength(0)
                .ofMaxLength(5000);
        Arbitrary<List<String>> tags = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20)
                .list()
                .ofMinSize(5)
                .ofMaxSize(15);

        return Combinators.combine(titles, descriptions, tags)
                .as(MetadataUpdateParams::new);
    }

    // ========== Property 5b: Metadata Confirmation Invariant ==========

    /**
     * Property 5b: 元数据确认后不可编辑
     * <p>
     * 验证：执行 confirm() 后，元数据变为已确认状态，后续 update() 操作应抛出异常。
     */
    @Property(tries = 100)
    @Label("Feature: video-distribution-platform, Property 5b: 元数据确认后不可编辑")
    void property_5b_metadataConfirmedCannotBeEdited(
            @ForAll("validMetadataCreationParams") MetadataCreationParams params,
            @ForAll("validMetadataUpdateParams") MetadataUpdateParams updateParams) {
        // Given: 创建一个元数据并确认
        VideoMetadata metadata = VideoMetadata.create(
                params.videoId(),
                params.title(),
                params.description(),
                params.tags(),
                params.source()
        );
        metadata.confirm();

        // Then: 确认后状态应为 true
        assertThat(metadata.isConfirmed()).isTrue();

        // And: 再次更新应抛出异常
        assertThatThrownBy(() -> metadata.update(updateParams.title(), updateParams.description(), updateParams.tags()))
                .isInstanceOf(com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException.class)
                .hasMessageContaining("已确认");
    }

    // ========== Record/Helper Classes ==========

    /**
     * 元数据创建参数记录
     */
    private record MetadataCreationParams(
            VideoId videoId,
            String title,
            String description,
            List<String> tags,
            MetadataSource source
    ) {}

    /**
     * 元数据更新参数记录
     */
    private record MetadataUpdateParams(
            String title,
            String description,
            List<String> tags
    ) {}
}
