package com.grace.platform.video;

import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;
import com.grace.platform.video.domain.UploadSession;
import com.grace.platform.video.domain.Video;
import com.grace.platform.video.domain.VideoFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Video domain entities.
 * <p>
 * Covers boundary conditions and error scenarios:
 * - Invalid video format (error code 1001)
 * - File size exceeding 5GB (error code 1002)
 * - Chunk index out of range (error code 1005)
 * - Duplicate chunk upload (error code 1006)
 * - Incomplete upload completion (error code 1007)
 *
 * @author Grace Platform Team
 */
class VideoUnitTest {

    // ========== Error Code 1001: Invalid Video Format ==========

    @Test
    @DisplayName("空格式应抛出错误码 1001")
    void shouldThrowError1001ForNullFormat() {
        assertThatThrownBy(() -> Video.create(
                "test.mp4",
                1000L,
                null,
                Duration.ofSeconds(60),
                "/storage/test.mp4"
        ))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UNSUPPORTED_VIDEO_FORMAT));
    }

    @Test
    @DisplayName("所有支持的视频格式应被正确接受")
    void shouldAcceptAllSupportedFormats() {
        for (VideoFormat format : Video.SUPPORTED_FORMATS) {
            assertThatCode(() -> Video.create(
                    "test." + format.name().toLowerCase(),
                    1000L,
                    format,
                    Duration.ofSeconds(60),
                    "/storage/test." + format.name().toLowerCase()
            )).as("Format %s should be accepted", format).doesNotThrowAnyException();
        }
    }

    // ========== Error Code 1002: File Size Exceeded ==========

    @Test
    @DisplayName("文件大小超过 5GB 应抛出错误码 1002")
    void shouldThrowError1002ForFileOver5GB() {
        long over5GB = 5L * 1024 * 1024 * 1024 + 1; // 5GB + 1 byte
        assertThatThrownBy(() -> Video.create(
                "test.mp4",
                over5GB,
                VideoFormat.MP4,
                Duration.ofSeconds(60),
                "/storage/test.mp4"
        ))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.VIDEO_FILE_SIZE_EXCEEDED))
                .hasMessageContaining("5GB");
    }

    @Test
    @DisplayName("文件大小恰好等于 5GB 应被接受")
    void shouldAcceptExactly5GB() {
        long exactly5GB = 5L * 1024 * 1024 * 1024; // Exactly 5GB
        assertThatCode(() -> Video.create(
                "test.mp4",
                exactly5GB,
                VideoFormat.MP4,
                Duration.ofSeconds(60),
                "/storage/test.mp4"
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("文件大小为 0 应抛出错误码 1002")
    void shouldThrowError1002ForZeroSize() {
        assertThatThrownBy(() -> Video.create(
                "test.mp4",
                0L,
                VideoFormat.MP4,
                Duration.ofSeconds(60),
                "/storage/test.mp4"
        ))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.VIDEO_FILE_SIZE_EXCEEDED));
    }

    @Test
    @DisplayName("负文件大小应抛出错误码 1002")
    void shouldThrowError1002ForNegativeSize() {
        assertThatThrownBy(() -> Video.create(
                "test.mp4",
                -1L,
                VideoFormat.MP4,
                Duration.ofSeconds(60),
                "/storage/test.mp4"
        ))
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.VIDEO_FILE_SIZE_EXCEEDED));
    }

    // ========== Error Code 1005: Chunk Index Out of Range ==========

    @Test
    @DisplayName("分片索引为负数时应无效")
    void shouldRejectNegativeChunkIndex() {
        UploadSession session = UploadSession.create(
                "test.mp4",
                10_000_000L,
                VideoFormat.MP4,
                "/temp",
                null
        );
        assertThat(session.isValidChunkIndex(-1)).isFalse();
    }

    @Test
    @DisplayName("分片索引等于总分片数时应无效")
    void shouldRejectChunkIndexEqualToTotal() {
        UploadSession session = UploadSession.create(
                "test.mp4",
                10_000_000L,
                VideoFormat.MP4,
                "/temp",
                null
        );
        int totalChunks = session.getTotalChunks();
        assertThat(session.isValidChunkIndex(totalChunks)).isFalse();
    }

    @Test
    @DisplayName("分片索引大于总分片数时应无效")
    void shouldRejectChunkIndexGreaterThanTotal() {
        UploadSession session = UploadSession.create(
                "test.mp4",
                10_000_000L,
                VideoFormat.MP4,
                "/temp",
                null
        );
        int totalChunks = session.getTotalChunks();
        assertThat(session.isValidChunkIndex(totalChunks + 1)).isFalse();
    }

    @Test
    @DisplayName("分片索引在有效范围内时应有效")
    void shouldAcceptValidChunkIndex() {
        UploadSession session = UploadSession.create(
                "test.mp4",
                10_000_000L,
                VideoFormat.MP4,
                "/temp",
                null
        );
        assertThat(session.isValidChunkIndex(0)).isTrue();
        assertThat(session.isValidChunkIndex(session.getTotalChunks() - 1)).isTrue();
    }

    // ========== Error Code 1006: Duplicate Chunk ==========

    @Test
    @DisplayName("上传分片数超过总分片数时应抛出错误码 1007")
    void shouldPreventDuplicateChunkUpload() {
        UploadSession session = UploadSession.create(
                "test.mp4",
                10_000_000L,
                VideoFormat.MP4,
                "/temp",
                null
        );
        int totalChunks = session.getTotalChunks();

        // Upload all chunks
        for (int i = 0; i < totalChunks; i++) {
            session.incrementUploadedChunks();
        }

        // Try to upload one more - should fail with UPLOAD_NOT_COMPLETE
        assertThatThrownBy(() -> session.incrementUploadedChunks())
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UPLOAD_NOT_COMPLETE));
    }

    @Test
    @DisplayName("已上传分片数不应超过总分片数")
    void uploadedChunksShouldNotExceedTotal() {
        UploadSession session = UploadSession.create(
                "test.mp4",
                5L * 1024 * 1024, // 5MB file = 1 chunk
                VideoFormat.MP4,
                "/temp",
                null
        );

        assertThat(session.getTotalChunks()).isEqualTo(1);

        // Upload the only chunk
        session.incrementUploadedChunks();
        assertThat(session.getUploadedChunks()).isEqualTo(1);
        assertThat(session.isUploadComplete()).isTrue();

        // Verify we can't upload more
        assertThatThrownBy(() -> session.incrementUploadedChunks())
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UPLOAD_NOT_COMPLETE));
    }

    // ========== Error Code 1007: Upload Not Complete ==========

    @Test
    @DisplayName("未完成所有分片时标记完成应抛出错误码 1007")
    void shouldThrowError1007WhenCompletingIncompleteUpload() {
        UploadSession session = UploadSession.create(
                "test.mp4",
                15_000_000L, // 15MB = 3 chunks of 5MB each
                VideoFormat.MP4,
                "/temp",
                null
        );

        // Upload only 1 of 3 chunks
        session.incrementUploadedChunks();
        assertThat(session.isUploadComplete()).isFalse();

        // Try to mark as completed - should fail
        assertThatThrownBy(() -> session.markAsCompleted())
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UPLOAD_NOT_COMPLETE))
                .hasMessageContaining("incomplete")
                .hasMessageContaining("1")
                .hasMessageContaining("3");
    }

    @Test
    @DisplayName("未上传任何分片时标记完成应抛出错误码 1007")
    void shouldThrowError1007WhenCompletingWithZeroChunks() {
        UploadSession session = UploadSession.create(
                "test.mp4",
                10_000_000L,
                VideoFormat.MP4,
                "/temp",
                null
        );

        assertThat(session.getUploadedChunks()).isEqualTo(0);
        assertThat(session.isUploadComplete()).isFalse();

        assertThatThrownBy(() -> session.markAsCompleted())
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UPLOAD_NOT_COMPLETE));
    }

    @Test
    @DisplayName("完成所有分片后标记完成应成功")
    void shouldCompleteWhenAllChunksUploaded() {
        UploadSession session = UploadSession.create(
                "test.mp4",
                5L * 1024 * 1024, // 5MB = 1 chunk
                VideoFormat.MP4,
                "/temp",
                null
        );

        // Upload the only chunk
        session.incrementUploadedChunks();
        assertThat(session.isUploadComplete()).isTrue();

        // Mark as completed - should succeed
        assertThatCode(() -> session.markAsCompleted()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("已完成的会话再次标记完成应抛出错误码 1007")
    void shouldThrowError1007WhenCompletingAlreadyCompletedSession() {
        UploadSession session = UploadSession.create(
                "test.mp4",
                5L * 1024 * 1024,
                VideoFormat.MP4,
                "/temp",
                null
        );

        // Complete the upload
        session.incrementUploadedChunks();
        session.markAsCompleted();

        // Try to mark as completed again - should fail
        assertThatThrownBy(() -> session.markAsCompleted())
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UPLOAD_NOT_COMPLETE));
    }

    // ========== Additional Boundary Tests ==========

    @Test
    @DisplayName("过期会话上传分片应抛出错误码 1004")
    void shouldThrowError1004ForExpiredSession() {
        UploadSession session = UploadSession.create(
                "test.mp4",
                10_000_000L,
                VideoFormat.MP4,
                "/temp",
                null
        );

        // Manually set expiration to past
        java.lang.reflect.Field expiresAtField;
        try {
            expiresAtField = UploadSession.class.getDeclaredField("expiresAt");
            expiresAtField.setAccessible(true);
            expiresAtField.set(session, java.time.LocalDateTime.now().minusHours(1));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertThat(session.isExpired()).isTrue();

        assertThatThrownBy(() -> session.incrementUploadedChunks())
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UPLOAD_SESSION_EXPIRED));
    }

    @Test
    @DisplayName("过期会话标记完成应抛出错误码 1004")
    void shouldThrowError1004WhenCompletingExpiredSession() {
        UploadSession session = UploadSession.create(
                "test.mp4",
                10_000_000L,
                VideoFormat.MP4,
                "/temp",
                null
        );

        // Upload all chunks first
        while (!session.isUploadComplete()) {
            session.incrementUploadedChunks();
        }

        // Manually set expiration to past
        java.lang.reflect.Field expiresAtField;
        try {
            expiresAtField = UploadSession.class.getDeclaredField("expiresAt");
            expiresAtField.setAccessible(true);
            expiresAtField.set(session, java.time.LocalDateTime.now().minusHours(1));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertThat(session.isExpired()).isTrue();

        assertThatThrownBy(() -> session.markAsCompleted())
                .isInstanceOf(BusinessRuleViolationException.class)
                .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                        .isEqualTo(ErrorCode.UPLOAD_SESSION_EXPIRED));
    }

    @Test
    @DisplayName("文件大小边界测试 - 刚好低于 5GB 应被接受")
    void shouldAcceptJustUnder5GB() {
        long justUnder5GB = 5L * 1024 * 1024 * 1024 - 1;
        assertThatCode(() -> Video.create(
                "test.mp4",
                justUnder5GB,
                VideoFormat.MP4,
                Duration.ofSeconds(60),
                "/storage/test.mp4"
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("空文件名应抛出错误")
    void shouldRejectBlankFileName() {
        assertThatThrownBy(() -> Video.create(
                "",
                1000L,
                VideoFormat.MP4,
                Duration.ofSeconds(60),
                "/storage/test.mp4"
        ))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("空文件路径应抛出错误")
    void shouldRejectBlankFilePath() {
        assertThatThrownBy(() -> Video.create(
                "test.mp4",
                1000L,
                VideoFormat.MP4,
                Duration.ofSeconds(60),
                ""
        ))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
