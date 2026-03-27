package com.grace.platform.video;

import com.grace.platform.video.domain.Video;
import com.grace.platform.video.domain.VideoFormat;
import com.grace.platform.video.domain.VideoStatus;
import net.jqwik.api.*;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for Video domain.
 * <p>
 * Covers Properties 1-2 from testing strategy:
 * - Property 1: Video file info extraction correctness
 * - Property 2: Video format validation boundaries
 * <p>
 * Note: Property 3 (persistence round-trip) is tested in VideoIntegrationTest
 * as it requires Spring context and database access.
 *
 * @author Grace Platform Team
 */
class VideoPropertyTest {

    // ========== Property 1: Video File Info Extraction Correctness ==========

    /**
     * Property 1: 视频文件信息提取正确性
     * <p>
     * 验证：使用任意有效视频文件参数创建 Video 实体后，
     * 所有字段值应与输入参数一致。
     */
    @Property(tries = 100)
    @Label("Feature: video-distribution-platform, Property 1: 视频文件信息提取正确性")
    void property_1_videoFileInfoExtraction(
            @ForAll("validVideoCreationParams") VideoCreationParams params) {
        // When: 创建 Video 实体
        Video video = Video.create(
                params.fileName(),
                params.fileSize(),
                params.format(),
                params.duration(),
                params.filePath()
        );

        // Then: 提取的信息与输入参数一致
        assertThat(video.getFileName()).isEqualTo(params.fileName());
        assertThat(video.getFileSize()).isEqualTo(params.fileSize());
        assertThat(video.getFormat()).isEqualTo(params.format());
        assertThat(video.getDuration()).isEqualTo(params.duration());
        assertThat(video.getFilePath()).isEqualTo(params.filePath());
        assertThat(video.getStatus()).isEqualTo(VideoStatus.UPLOADED);
        assertThat(video.getId()).isNotNull();
    }

    @Provide
    Arbitrary<VideoCreationParams> validVideoCreationParams() {
        Arbitrary<VideoFormat> formats = Arbitraries.of(VideoFormat.values());
        Arbitrary<Long> sizes = Arbitraries.longs().between(1, 5L * 1024 * 1024 * 1024);
        Arbitrary<Long> durationSeconds = Arbitraries.longs().between(1, 86400);
        Arbitrary<String> names = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(100)
                .map(name -> name + ".mp4");
        Arbitrary<String> paths = Arbitraries.strings()
                .alpha()
                .ofMinLength(10)
                .ofMaxLength(200)
                .map(path -> "/storage/videos/" + path + ".mp4");

        return Combinators.combine(names, sizes, formats, durationSeconds, paths)
                .as((name, size, format, durSecs, path) ->
                        new VideoCreationParams(name, size, format, Duration.ofSeconds(durSecs), path));
    }

    // ========== Property 2: Video Format Validation Boundaries ==========

    /**
     * Property 2: 视频格式验证边界
     * <p>
     * 验证：支持 MP4、MOV、AVI、MKV 格式，其他格式应抛出异常。
     */
    @Property(tries = 100)
    @Label("Feature: video-distribution-platform, Property 2: 视频格式验证边界")
    void property_2_videoFormatValidation(
            @ForAll("anyFileExtension") String extension) {
        Set<String> supportedFormats = Set.of("MP4", "MOV", "AVI", "MKV");
        boolean isSupported = supportedFormats.contains(extension.toUpperCase());

        // Try to parse the format
        VideoFormat parsedFormat = parseFormatOrNull(extension);

        if (isSupported && parsedFormat != null) {
            // Then: 应接受该文件
            final VideoFormat format = parsedFormat;
            assertThatCode(() -> Video.create(
                    "test." + extension,
                    1000L,
                    format,
                    Duration.ofSeconds(60),
                    "/storage/test." + extension
            )).doesNotThrowAnyException();
        } else {
            // Then: 无法创建（格式枚举中不存在）
            assertThat(parsedFormat).isNull();
        }
    }

    private VideoFormat parseFormatOrNull(String extension) {
        try {
            return VideoFormat.valueOf(extension.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Provide
    Arbitrary<String> anyFileExtension() {
        // 混合支持的格式和随机格式
        return Arbitraries.oneOf(
                Arbitraries.of("mp4", "mov", "avi", "mkv"),
                Arbitraries.of("wmv", "flv", "webm", "3gp", "mpeg"),
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(5)
        );
    }

    /**
     * Property 2b: 支持的视频格式验证
     * <p>
     * 验证：所有 VideoFormat 枚举值都应该被 Video.SUPPORTED_FORMATS 支持
     */
    @Property(tries = 100)
    @Label("Feature: video-distribution-platform, Property 2b: 支持格式枚举一致性")
    void property_2b_supportedFormatConsistency(@ForAll VideoFormat format) {
        assertThat(Video.SUPPORTED_FORMATS).contains(format);

        // 验证可以创建
        assertThatCode(() -> Video.create(
                "test." + format.name().toLowerCase(),
                1000L,
                format,
                Duration.ofSeconds(60),
                "/storage/test." + format.name().toLowerCase()
        )).doesNotThrowAnyException();
    }

    // ========== Record/Helper Classes ==========

    /**
     * 视频创建参数记录
     */
    private record VideoCreationParams(
            String fileName,
            long fileSize,
            VideoFormat format,
            Duration duration,
            String filePath
    ) {}
}
