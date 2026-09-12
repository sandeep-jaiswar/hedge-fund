package com.hedgefund.testsupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DatalakeTestHelper {

    private final Path root;

    public DatalakeTestHelper() {
        this.root = createTempDatalake();
    }

    public DatalakeTestHelper(Path root) {
        this.root = root;
    }

    public Path root() {
        return root;
    }

    public Path bronzeDir(String source) {
        return root.resolve("data/bronze/" + source);
    }

    public Path silverDir(String source) {
        return root.resolve("data/silver/" + source);
    }

    public Path catalogFile() {
        return root.resolve("catalog/glue.json");
    }

    public void writeBronzeNdjson(String source, String symbol, String content) throws IOException {
        Path dir = bronzeDir(source).resolve("symbol=" + symbol);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("data.ndjson"), content);
    }

    public void writeBronzeCsv(String source, String symbol, String content) throws IOException {
        Path dir = bronzeDir(source).resolve("symbol=" + symbol);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("data.csv"), content);
    }

    public void cleanup() {
        try {
            deleteRecursive(root);
        } catch (IOException e) {
            // ignore cleanup failures
        }
    }

    private static Path createTempDatalake() {
        try {
            Path temp = Files.createTempDirectory("hedge-test-datalake-");
            Files.createDirectories(temp.resolve("catalog"));
            Files.createDirectories(temp.resolve("data/bronze"));
            Files.createDirectories(temp.resolve("data/silver"));
            Files.createDirectories(temp.resolve("data/gold"));
            return temp;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp datalake", e);
        }
    }

    private void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                for (Path child : stream.toList()) {
                    deleteRecursive(child);
                }
            }
        }
        Files.deleteIfExists(path);
    }
}
