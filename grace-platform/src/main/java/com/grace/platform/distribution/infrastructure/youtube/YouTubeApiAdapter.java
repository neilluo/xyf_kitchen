package com.grace.platform.distribution.infrastructure.youtube;

import java.nio.file.Path;
import java.util.List;

/**
 * YouTube API 适配器接口
 * <p>
 * 封装 YouTube Data API v3 的上传操作，提供断点续传支持。
 * 每日配额：10,000 units；单次上传消耗：1,600 units。
 * </p>
 *
 * @author Grace Platform Team
 * @see YouTubeApiAdapterImpl
 */
public interface YouTubeApiAdapter {

    /**
     * 使用断点续传协议上传视频
     * <p>
     * 发起可恢复上传会话，返回上传结果包含任务ID和上传URI。
     * 若上传过程中断，可使用 {@link #resumeUpload} 方法恢复。
     * </p>
     *
     * @param accessToken  OAuth 访问令牌
     * @param videoFile    视频文件路径
     * @param title        视频标题
     * @param description  视频描述
     * @param tags         标签列表
     * @param privacyStatus 隐私状态（public/unlisted/private）
     * @return 上传结果，包含上传URI和任务信息
     */
    YouTubeUploadResult uploadVideo(String accessToken, Path videoFile,
                                     String title, String description,
                                     List<String> tags, String privacyStatus);

    /**
     * 查询上传进度
     * <p>
     * 根据上传URI查询当前上传进度和状态。
     * </p>
     *
     * @param accessToken OAuth 访问令牌
     * @param uploadUri   上传会话URI
     * @return 上传进度信息
     */
    YouTubeUploadProgress getUploadProgress(String accessToken, String uploadUri);

    /**
     * 恢复中断的上传
     * <p>
     * 当上传因配额超限、网络中断等原因失败后，使用此方法恢复上传。
     * 实现会查询服务器已接收的字节数，从中断位置继续传输。
     * </p>
     *
     * @param accessToken OAuth 访问令牌
     * @param uploadUri   上传会话URI
     * @param videoFile   视频文件路径
     * @return 恢复后的上传结果
     */
    YouTubeUploadResult resumeUpload(String accessToken, String uploadUri, Path videoFile);
}
