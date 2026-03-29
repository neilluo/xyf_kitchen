package com.grace.platform.metadata.domain;

import com.grace.platform.video.domain.VideoFileInfo;
import java.nio.file.Path;
import java.util.List;

/**
 * 元数据生成领域服务接口
 * <p>
 * 定义在 domain 层，由 infrastructure 层的 MetadataGenerationServiceImpl 实现
 * 内部调用 LlmService 完成实际的 AI 生成逻辑。
 * <p>
 * 支持两种模式：
 * - 纯文本模式：基于文件名和历史元数据风格生成
 * - 多模态模式：提取视频关键帧，基于实际视频内容生成
 * </p>
 */
public interface MetadataGenerationService {

    /**
     * 根据视频文件信息和历史元数据生成视频元数据
     * <p>
     * 纯文本模式，基于文件名和历史元数据风格生成。
     *
     * @param videoId            视频 ID
     * @param videoInfo          视频文件信息
     * @param historicalMetadata 历史元数据列表（用于风格参考）
     * @return 生成的视频元数据
     */
    VideoMetadata generate(com.grace.platform.shared.domain.id.VideoId videoId, VideoFileInfo videoInfo, List<VideoMetadata> historicalMetadata);

    /**
     * 根据视频文件信息、历史元数据和视频路径生成视频元数据
     * <p>
     * 多模态模式，提取视频关键帧作为视觉输入，基于实际视频内容生成更准确的元数据。
     *
     * @param videoId            视频 ID
     * @param videoInfo          视频文件信息
     * @param historicalMetadata 历史元数据列表（用于风格参考）
     * @param videoPath          视频文件路径（用于提取关键帧）
     * @return 生成的视频元数据
     */
    VideoMetadata generate(com.grace.platform.shared.domain.id.VideoId videoId, VideoFileInfo videoInfo, List<VideoMetadata> historicalMetadata, Path videoPath);
}
