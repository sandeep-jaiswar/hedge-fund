package com.hedgefund.yahoo.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedgefund.yahoo.model.Bar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class YahooBronzeWriter {

    private final Path bronzeRoot;
    private final ObjectMapper om = new ObjectMapper();

    public YahooBronzeWriter(Path bronzeRoot) {
        this.bronzeRoot = bronzeRoot;
    }

    public Path write(String symbol, List<Bar> bars, String rawJson) throws IOException {
        Path dir = bronzeRoot.resolve("symbol=" + symbol);
        Files.createDirectories(dir);

        if (rawJson != null) {
            Path rawPath = dir.resolve("_raw.json");
            atomicWrite(rawPath, rawJson);
        }

        StringBuilder sb = new StringBuilder();
        for (Bar b : bars) {
            sb.append(om.writeValueAsString(b)).append("\n");
        }

        Path out = dir.resolve("data.ndjson");
        atomicWrite(out, sb.toString());
        return out;
    }

    private void atomicWrite(Path target, String content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, content);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
