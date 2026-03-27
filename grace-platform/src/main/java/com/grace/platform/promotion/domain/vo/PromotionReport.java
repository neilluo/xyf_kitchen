package com.grace.platform.promotion.domain.vo;

import com.grace.platform.shared.domain.id.VideoId;
import java.util.List;

public record PromotionReport(
    VideoId videoId,
    String videoTitle,
    int totalChannels,
    int successCount,
    int failedCount,
    int pendingCount,
    double overallSuccessRate,
    List<ChannelExecutionSummary> channelSummaries
) {}
