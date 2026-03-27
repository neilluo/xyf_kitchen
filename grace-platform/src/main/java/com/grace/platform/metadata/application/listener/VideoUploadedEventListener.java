package com.grace.platform.metadata.application.listener;

import com.grace.platform.metadata.application.MetadataApplicationService;
import com.grace.platform.video.domain.event.VideoUploadedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 视频上传完成事件监听器
 * <p>
 * 监听 {@link VideoUploadedEvent}，当视频上传完成后自动触发元数据生成流程。
 * 对应文档 §C.2 的时序图逻辑。
 * </p>
 */
@Component
public class VideoUploadedEventListener {

    private final MetadataApplicationService metadataApplicationService;

    /**
     * 创建事件监听器
     *
     * @param metadataApplicationService 元数据应用服务
     */
    public VideoUploadedEventListener(MetadataApplicationService metadataApplicationService) {
        this.metadataApplicationService = metadataApplicationService;
    }

    /**
     * 处理视频上传完成事件
     * <p>
     * 自动调用元数据生成服务，为上传完成的视频生成 AI 元数据。
     * </p>
     *
     * @param event 视频上传完成事件
     */
    @EventListener
    public void handle(VideoUploadedEvent event) {
        metadataApplicationService.generateMetadata(event.getVideoId());
    }
}
