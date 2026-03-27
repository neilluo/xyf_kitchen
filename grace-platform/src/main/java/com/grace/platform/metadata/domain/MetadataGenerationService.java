package com.grace.platform.metadata.domain;

import com.grace.platform.video.domain.VideoFileInfo;
import java.util.List;

/**
 * 元数据生成领域服务接口
 * <p>
 * 定义在 domain 层，由 infrastructure 层的 MetadataGenerationServiceImpl 实现
 * 内部调用 LlmService 完成实际的 AI 生成逻辑。
 * </p>
 */
public interface MetadataGenerationService {

    /**
     * 根据视频文件信息和历史元数据生成视频元数据
     *
     * @param videoInfo          视频文件信息
     * @param historicalMetadata 历史元数据列表（用于风格参考）
     * @return 生成的视频元数据
     */
    VideoMetadata generate(VideoFileInfo videoInfo, List<VideoMetadata> historicalMetadata);
}
