package com.hedgefund.ingest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class GenericBronzeWriter {
    private final Path bronzeRoot;
    public GenericBronzeWriter(Path bronzeRoot) { this.bronzeRoot = bronzeRoot; }

    public Path write(String key, String raw) throws Exception {
        String safe = key.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path dir = bronzeRoot.resolve("key=" + safe);
        Files.createDirectories(dir);
        Path p = dir.resolve("data.raw");
        Path tmp = p.resolveSibling(p.getFileName() + ".tmp");
        Files.writeString(tmp, raw);
        Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return p;
    }
}
