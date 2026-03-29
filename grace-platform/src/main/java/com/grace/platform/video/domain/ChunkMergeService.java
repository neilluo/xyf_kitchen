package com.grace.platform.video.domain;

import java.nio.file.Path;

/**
 * 分片合并服务领域服务接口。
 * <p>
 * 将临时目录中的分片文件按索引顺序合并为完整视频文件。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public interface ChunkMergeService {

    /**
     * 合并分片文件为完整视频文件。
     *
     * @param tempDirectory  临时分片存储目录
     * @param totalChunks    总分片数
     * @param targetFileName 目标文件名
     * @return 合并后的视频文件路径
     */
    Path mergeChunks(Path tempDirectory, int totalChunks, String targetFileName);
}