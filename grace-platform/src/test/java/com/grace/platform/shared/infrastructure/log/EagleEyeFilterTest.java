package com.grace.platform.shared.infrastructure.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EagleEyeFilterTest {

    @Test
    void testTraceInitFailureDoesNotBlockRequest() throws ServletException, IOException {
        // Arrange
        EagleEyeFilter filter = new EagleEyeFilter();
        ReflectionTestUtils.setField(filter, "appName", "grace-platform");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // Mock TraceContext.startTrace to throw exception
        try (MockedStatic<TraceContext> mocked = mockStatic(TraceContext.class)) {
            mocked.when(() -> TraceContext.startTrace(any(HttpServletRequest.class), anyString()))
                    .thenThrow(new RuntimeException("Trace init failed"));

            // Act
            filter.doFilterInternal(request, response, chain);

            // Assert: chain.doFilter should still be called
            verify(chain, times(1)).doFilter(request, response);

            // Assert: TraceContext.clear should still be called
            mocked.verify(() -> TraceContext.clear(), times(1));
        }
    }
}
