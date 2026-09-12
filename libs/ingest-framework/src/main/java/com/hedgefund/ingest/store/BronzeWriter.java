package com.hedgefund.ingest.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class BronzeWriter {

    private static final Logger log = LoggerFactory.getLogger(BronzeWriter.class);

    private final Path bronzeRoot;

    public BronzeWriter(Path bronzeRoot) {
        this.bronzeRoot = bronzeRoot;
    }

    public Path writeNdjson(String key, String content) throws IOException {
        Path dir = bronzeRoot.resolve("symbol=" + key);
        Files.createDirectories(dir);
        Path out = dir.resolve("data.ndjson");
        atomicWrite(out, content);
        log.debug("Bronze wrote {} bytes to {}", content.length(), out);
        return out;
    }

    public Path writeRawJson(String key, String rawJson) throws IOException {
        Path dir = bronzeRoot.resolve("symbol=" + key);
        Files.createDirectories(dir);
        Path out = dir.resolve("_raw.json");
        atomicWrite(out, rawJson);
        return out;
    }

    public Path writeCsv(String key, String csvContent) throws IOException {
        Path dir = bronzeRoot.resolve("symbol=" + key);
        Files.createDirectories(dir);
        Path out = dir.resolve("data.csv");
        atomicWrite(out, csvContent);
        return out;
    }

    public Path writeWatermark(int symbolCount) throws IOException {
        Path out = bronzeRoot.resolve("_watermark.json");
        String json = "{\"lastRun\":\"" + java.time.Instant.now() + "\",\"symbols\":" + symbolCount + "}";
        atomicWrite(out, json);
        return out;
    }

    private void atomicWrite(Path target, String content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, content);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
