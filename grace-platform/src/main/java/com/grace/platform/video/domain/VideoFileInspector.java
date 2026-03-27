package com.grace.platform.video.domain;

import java.nio.file.Path;

/**
 * 视频文件检查器领域服务接口。
 * <p>
 * 提取合并后视频文件的元信息（时长、分辨率等）。定义在 domain 层，
 * 由 infrastructure 层实现（可使用 FFprobe 或 JavaCV）。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public interface VideoFileInspector {

    /**
     * 检查视频文件，提取元信息。
     *
     * @param filePath 视频文件路径
     * @return 视频文件信息（包含文件名、文件大小、格式、时长等）
     */
    VideoFileInfo inspect(Path filePath);
}
