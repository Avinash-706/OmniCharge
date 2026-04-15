package com.omnicharge.common.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class FallbackLogWriterTest {
    @TempDir
    Path tempDir;
    private FallbackLogWriter writer;

    @BeforeEach
    void setUp() {
        writer = new FallbackLogWriter();
        ReflectionTestUtils.setField(writer, "fallbackDir", tempDir.toString());
        ReflectionTestUtils.setField(writer, "serviceName", "test-service");
    }

    @Test
    void writeToFallbackFile_shouldCreateFileAndWriteEvent() {
        LogEvent event = LogEvent.builder().level("ERROR").message("Test error").timestamp(LocalDateTime.now()).build();
        
        writer.writeToFallbackFile(event);
        
        Path fallbackFile = tempDir.resolve("fallback-buffer-test-service.log");
        assertThat(fallbackFile).exists();
    }
}
