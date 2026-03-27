package com.grace.platform.promotion.domain;

public record PromotionResult(
    PromotionStatus status,
    String resultUrl,
    String errorMessage
) {}
