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

    /**
     * 从 URL 检查视频文件，提取元信息。
     * <p>
     * 用于 OSS 直传场景，从 OSS URL 提取视频信息。
     * 实现可能需要下载到临时文件后提取，或使用 OSS 视频处理 API。
     *
     * @param url 视频 URL（OSS 或其他存储 URL）
     * @return 视频文件信息（包含时长等）
     */
    VideoFileInfo inspectFromUrl(String url);
}
