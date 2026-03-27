package com.grace.platform.distribution.application.listener;

import com.grace.platform.distribution.application.DistributionApplicationService;
import com.grace.platform.distribution.application.command.PublishCommand;
import com.grace.platform.metadata.domain.event.MetadataConfirmedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 元数据确认事件监听器
 * <p>
 * 监听 {@link MetadataConfirmedEvent}，当元数据被用户确认后
 * 可自动触发视频发布流程（可选行为，根据业务需求配置）。
 * </p>
 * <p>
 * 此监听器为 Distribution 上下文与 Metadata 上下文的集成点，
 * 负责响应上游元数据确认事件。
 * </p>
 */
@Component
public class MetadataConfirmedEventListener {

    private final DistributionApplicationService distributionApplicationService;

    /**
     * 创建事件监听器
     *
     * @param distributionApplicationService 分发应用服务
     */
    public MetadataConfirmedEventListener(DistributionApplicationService distributionApplicationService) {
        this.distributionApplicationService = distributionApplicationService;
    }

    /**
     * 处理元数据确认事件
     * <p>
     * 当元数据被确认后，视频状态更新为 READY_TO_PUBLISH。
     * 注意：此监听器目前仅记录事件，不自动触发发布流程。
     * 发布操作由用户在前端界面主动发起（调用 publish API）。
     * </p>
     *
     * @param event 元数据确认事件
     */
    @EventListener
    public void handle(MetadataConfirmedEvent event) {
        // 记录元数据已确认，视频已就绪
        // 实际发布操作由用户主动发起，不在这里自动调用 publish
        // 如需自动发布，可在此调用 distributionApplicationService.publish()
    }
}
