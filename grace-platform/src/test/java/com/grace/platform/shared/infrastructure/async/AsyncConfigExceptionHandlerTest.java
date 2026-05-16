package com.grace.platform.shared.infrastructure.async;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigExceptionHandlerTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger asyncConfigLogger;

    @BeforeEach
    void setUp() {
        asyncConfigLogger = (Logger) LoggerFactory.getLogger(AsyncConfig.class);
        appender = new ListAppender<>();
        appender.start();
        asyncConfigLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        asyncConfigLogger.detachAppender(appender);
    }

    @Test
    void uncaughtExceptionHandlerLogsErrorWithMethodNameAndStack() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler =
                new AsyncConfig().getAsyncUncaughtExceptionHandler();
        Method method = String.class.getMethod("toString");
        RuntimeException ex = new RuntimeException("boom");

        handler.handleUncaughtException(ex, method, "arg1", 42);

        assertThat(appender.list)
                .anyMatch(event -> event.getLevel() == Level.ERROR
                        && event.getFormattedMessage().contains("toString")
                        && event.getThrowableProxy() != null
                        && event.getThrowableProxy().getMessage().equals("boom"));
    }
}
