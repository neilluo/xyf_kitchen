package com.grace.platform.distribution.domain;

/**
 * 可恢复的视频分发器扩展接口
 * <p>
 * 扩展 {@link VideoDistributor} 接口，为支持断点续传的平台提供
 * 恢复上传的能力。YouTube 等平台实现此接口。
 * </p>
 *
 * @author Grace Platform Team
 * @see VideoDistributor
 */
public interface ResumableVideoDistributor extends VideoDistributor {

    /**
     * 从中断位置恢复上传
     * <p>
     * 当上传因网络中断、配额超限等原因失败后，使用此方法恢复上传。
     * 实现类应维护上传状态，从中断位置继续传输，避免重新上传整个文件。
     * </p>
     *
     * @param taskId 上传任务ID
     * @return 发布结果，包含恢复后的任务状态
     */
    PublishResult resumeUpload(String taskId);
}
