package com.hedgefund.ingest.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BronzeWriterTest {

    @Test
    void writeNdjsonCreatesFile(@TempDir Path tempDir) throws IOException {
        BronzeWriter writer = new BronzeWriter(tempDir);
        String content = "{\"symbol\":\"AAPL\",\"date\":\"2024-01-01\"}\n";

        Path result = writer.writeNdjson("AAPL", content);

        assertTrue(Files.exists(result));
        assertEquals("data.ndjson", result.getFileName().toString());
        assertEquals(content, Files.readString(result));
    }

    @Test
    void writeRawJsonCreatesFile(@TempDir Path tempDir) throws IOException {
        BronzeWriter writer = new BronzeWriter(tempDir);
        String json = "{\"raw\":true}";

        Path result = writer.writeRawJson("AAPL", json);

        assertTrue(Files.exists(result));
        assertEquals("_raw.json", result.getFileName().toString());
        assertEquals(json, Files.readString(result));
    }

    @Test
    void writeWatermarkCreatesFile(@TempDir Path tempDir) throws IOException {
        BronzeWriter writer = new BronzeWriter(tempDir);

        Path result = writer.writeWatermark(22);

        assertTrue(Files.exists(result));
        assertTrue(Files.readString(result).contains("\"symbols\":22"));
    }

    @Test
    void atomicWriteDoesNotLeaveTempFile(@TempDir Path tempDir) throws IOException {
        BronzeWriter writer = new BronzeWriter(tempDir);

        writer.writeNdjson("TEST", "test content");

        assertFalse(Files.exists(tempDir.resolve("symbol=TEST/data.ndjson.tmp")));
        assertTrue(Files.exists(tempDir.resolve("symbol=TEST/data.ndjson")));
    }
}
