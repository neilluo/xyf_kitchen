package com.grace.platform.distribution.domain;

/**
 * 视频分发器策略接口
 * <p>
 * Strategy 模式核心接口。每个视频平台（YouTube、抖音等）实现此接口，
 * 提供平台特定的视频发布逻辑。
 * </p>
 *
 * @author Grace Platform Team
 * @see ResumableVideoDistributor
 * @see VideoDistributorRegistry
 */
public interface VideoDistributor {

    /**
     * 返回平台标识
     * <p>
     * 用于在 Registry 中注册和查找分发器。标识应全局唯一，
     * 如 "youtube"、"douyin" 等。
     * </p>
     *
     * @return 平台标识字符串
     */
    String platform();

    /**
     * 发布视频到平台
     * <p>
     * 执行实际的视频上传操作，返回发布结果。实现类应处理平台特定的
     * 上传协议、认证和错误处理。
     * </p>
     * <p>
     * 视频从存储 URL 读取，支持本地文件路径或 OSS URL。
     * </p>
     *
     * @param storageUrl 视频存储 URL（本地路径或 OSS URL）
     * @param metadata   视频元数据（标题、描述、标签等）
     * @return 发布结果，包含任务ID和状态
     */
    PublishResult publish(String storageUrl, VideoMetadata metadata);

    /**
     * 查询上传状态
     * <p>
     * 根据任务ID查询视频上传的当前进度和状态。用于轮询上传进度
     * 或确认发布是否完成。
     * </p>
     *
     * @param taskId 上传任务ID（由 {@link #publish} 返回）
     * @return 当前上传状态
     */
    UploadStatus getUploadStatus(String taskId);
}
