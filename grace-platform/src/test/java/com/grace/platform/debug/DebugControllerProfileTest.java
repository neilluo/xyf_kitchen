package com.grace.platform.debug;

import com.grace.platform.debug.interfaces.rest.DebugController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DebugControllerProfileTest {

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = "spring.profiles.active=test")
    static class DefaultProfileTest {
        @Autowired ApplicationContext ctx;

        @Test
        void debugControllerNotLoadedInTestProfile() {
            assertThrows(NoSuchBeanDefinitionException.class,
                    () -> ctx.getBean(DebugController.class));
        }
    }

    @SpringBootTest
    @ActiveProfiles("dev")
    static class DevProfileTest {
        @Autowired ApplicationContext ctx;

        @Test
        void debugControllerLoadedInDevProfile() {
            assertNotNull(ctx.getBean(DebugController.class));
        }
    }
}
