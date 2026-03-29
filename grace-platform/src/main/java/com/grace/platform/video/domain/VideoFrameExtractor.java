package com.grace.platform.video.domain;

import java.nio.file.Path;
import java.util.List;

/**
 * 视频帧提取器领域服务接口。
 * <p>
 * 从视频文件中提取关键帧图像，用于多模态 AI 分析。
 * 定义在 domain 层，由 infrastructure 层实现（使用 FFmpeg）。
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
     * 从视频中提取关键帧。
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
     * 从视频中提取默认数量的关键帧（3 帧：开头、中间、结尾）。
     *
     * @param videoPath 视频文件路径
     * @return 提取的图像帧列表，按位置排序
     * @throws com.grace.platform.shared.infrastructure.exception.FileOperationException 如果文件不存在或提取失败
     */
    default List<ImageFrame> extractFrames(Path videoPath) {
        return extractFrames(videoPath, DEFAULT_FRAME_COUNT);
    }
}