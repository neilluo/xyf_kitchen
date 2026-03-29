package com.grace.platform.video.domain;

import java.util.Objects;

/**
 * 视频帧图像值对象。
 * <p>
 * 封装从视频中提取的单帧图像信息，包含 Base64 编码的图像数据和元信息。
 * </p>
 *
 * @param base64Data Base64 编码的图像数据（不含 data URI 前缀）
 * @param mimeType   图像 MIME 类型（如 image/jpeg）
 * @param position   帧在视频中的位置比例（0.0 = 开头，1.0 = 结尾）
 */
public record ImageFrame(
        String base64Data,
        String mimeType,
        double position
) {

    private static final String DEFAULT_MIME_TYPE = "image/jpeg";

    /**
     * 创建图像帧值对象
     *
     * @param base64Data Base64 编码的图像数据，不能为空
     * @param mimeType   图像 MIME 类型，不能为空
     * @param position   帧位置比例，必须在 [0.0, 1.0] 范围内
     * @throws NullPointerException     如果 base64Data 或 mimeType 为 null
     * @throws IllegalArgumentException 如果 position 不在有效范围内
     */
    public ImageFrame {
        Objects.requireNonNull(base64Data, "base64Data must not be null");
        Objects.requireNonNull(mimeType, "mimeType must not be null");

        if (position < 0.0 || position > 1.0) {
            throw new IllegalArgumentException("position must be between 0.0 and 1.0");
        }
    }

    /**
     * 创建 JPEG 格式的图像帧
     *
     * @param base64Data Base64 编码的图像数据
     * @param position   帧位置比例
     * @return JPEG 图像帧
     */
    public static ImageFrame jpeg(String base64Data, double position) {
        return new ImageFrame(base64Data, DEFAULT_MIME_TYPE, position);
    }

    /**
     * 获取 data URI 格式的图像数据
     *
     * @return data URI 格式字符串
     */
    public String toDataUri() {
        return "data:" + mimeType + ";base64," + base64Data;
    }

    /**
     * 判断是否为开头帧
     *
     * @return 如果位置为 0.0 返回 true
     */
    public boolean isStartFrame() {
        return position == 0.0;
    }

    /**
     * 判断是否为中间帧
     *
     * @return 如果位置在 (0.0, 1.0) 范围内返回 true
     */
    public boolean isMiddleFrame() {
        return position > 0.0 && position < 1.0;
    }

    /**
     * 判断是否为结尾帧
     *
     * @return 如果位置为 1.0 返回 true
     */
    public boolean isEndFrame() {
        return position == 1.0;
    }
}