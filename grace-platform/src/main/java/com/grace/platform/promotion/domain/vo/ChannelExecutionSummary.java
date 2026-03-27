package com.grace.platform.promotion.domain.vo;

import com.grace.platform.promotion.domain.PromotionMethod;
import com.grace.platform.promotion.domain.PromotionStatus;
import com.grace.platform.shared.domain.id.ChannelId;
import java.time.LocalDateTime;

public record ChannelExecutionSummary(
    ChannelId channelId,
    String channelName,
    String channelType,
    PromotionMethod method,
    PromotionStatus status,
    String resultUrl,
    String errorMessage,
    LocalDateTime executedAt
) {}
