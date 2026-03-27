package com.grace.platform.promotion.domain.vo;

import com.grace.platform.promotion.domain.PromotionMethod;
import com.grace.platform.shared.domain.id.ChannelId;

public record PromotionCopy(
    ChannelId channelId,
    String channelName,
    String channelType,
    String promotionTitle,
    String promotionBody,
    PromotionMethod recommendedMethod,
    String methodReason
) {}
