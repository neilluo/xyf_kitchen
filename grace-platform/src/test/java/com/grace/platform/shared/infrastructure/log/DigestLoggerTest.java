package com.grace.platform.shared.infrastructure.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class DigestLoggerTest {

    private ListAppender<ILoggingEvent> llmAppender;
    private ListAppender<ILoggingEvent> fallbackAppender;
    private Logger llmLogger;
    private Logger fallbackLogger;

    @BeforeEach
    void setUp() {
        llmLogger = (Logger) LoggerFactory.getLogger("LLM_DIGEST");
        fallbackLogger = (Logger) LoggerFactory.getLogger("DIGEST_FALLBACK");
        llmAppender = new ListAppender<>();
        fallbackAppender = new ListAppender<>();
        llmAppender.start();
        fallbackAppender.start();
        llmLogger.addAppender(llmAppender);
        fallbackLogger.addAppender(fallbackAppender);
    }

    @AfterEach
    void tearDown() {
        llmLogger.detachAppender(llmAppender);
        fallbackLogger.detachAppender(fallbackAppender);
    }

    @Test
    void llmDigestWritesPipeSeparatedLine() {
        DigestLogger.llm("chat", "qwen-max", 1234, 567L,
                "OK", "-", "biz-001");

        assertThat(llmAppender.list)
                .anyMatch(e -> e.getLevel() == Level.INFO
                        && e.getFormattedMessage().equals(
                                "chat|qwen-max|1234|567|OK|-|biz-001"));
    }

    @Test
    void fallbackLoggerExistsAndIsWritable() {
        fallbackLogger.error("[DigestLoggerTest] sentinel message");

        assertThat(fallbackAppender.list)
                .anyMatch(e -> e.getLevel() == Level.ERROR
                        && e.getFormattedMessage().contains("sentinel message"));
    }
}
