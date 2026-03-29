package com.grace.platform.video.domain;

import java.nio.file.Path;
import java.util.List;

/**
 * 视频帧提取器领域服务接口。
 * <p>
 * 从视频文件中提取关键帧图像，用于多模态 AI 分析。
 * 定义在 domain 层，由 infrastructure 层实现（使用 FFmpeg）。
 * <p>
 * 支持两种输入方式：
 * - 本地文件路径（Path）：直接从本地文件提取
 * - URL 字符串：从 OSS 或 HTTP URL 下载到临时文件后提取
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public interface VideoFrameExtractor {

    /**
     * 默认提取帧数
     */
    int DEFAULT_FRAME_COUNT = 3;

    /**
     * 从本地视频中提取关键帧。
     * <p>
     * 提取视频的开头、中间、结尾三帧，转换为 Base64 编码的 JPEG 图像。
     *
     * @param videoPath  视频文件路径
     * @param frameCount 要提取的帧数（默认 3 帧：开头、中间、结尾）
     * @return 提取的图像帧列表，按位置排序
     * @throws com.grace.platform.shared.infrastructure.exception.FileOperationException 如果文件不存在或提取失败
     */
    List<ImageFrame> extractFrames(Path videoPath, int frameCount);

    /**
     * 从本地视频中提取默认数量的关键帧（3 帧：开头、中间、结尾）。
     *
     * @param videoPath 视频文件路径
     * @return 提取的图像帧列表，按位置排序
     * @throws com.grace.platform.shared.infrastructure.exception.FileOperationException 如果文件不存在或提取失败
     */
    default List<ImageFrame> extractFrames(Path videoPath) {
        return extractFrames(videoPath, DEFAULT_FRAME_COUNT);
    }

    /**
     * 从 URL（OSS 或 HTTP）提取视频关键帧。
     * <p>
     * 先将视频下载到临时文件，然后使用 FFmpeg 提取关键帧，
     * 提取完成后自动删除临时文件。
     * <p>
     * 支持的 URL 格式：
     * - OSS URL: https://{bucket}.{endpoint}/{objectKey}
     * - HTTP URL: http://...
     *
     * @param videoUrl   视频 URL（OSS 或 HTTP）
     * @param frameCount 要提取的帧数
     * @return 提取的图像帧列表，按位置排序
     * @throws com.grace.platform.shared.infrastructure.exception.FileOperationException 如果下载失败或提取失败
     */
    List<ImageFrame> extractFramesFromUrl(String videoUrl, int frameCount);

    /**
     * 从 URL 提取默认数量的关键帧（3 帧）。
     *
     * @param videoUrl 视频 URL
     * @return 提取的图像帧列表，按位置排序
     * @throws com.grace.platform.shared.infrastructure.exception.FileOperationException 如果下载失败或提取失败
     */
    default List<ImageFrame> extractFramesFromUrl(String videoUrl) {
        return extractFramesFromUrl(videoUrl, DEFAULT_FRAME_COUNT);
    }
}