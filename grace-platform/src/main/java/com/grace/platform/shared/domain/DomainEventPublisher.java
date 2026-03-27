package com.grace.platform.shared.domain;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
