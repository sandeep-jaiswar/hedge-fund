package com.hedgefund.ingest.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class SilverTransformer {

    private static final Logger log = LoggerFactory.getLogger(SilverTransformer.class);

    public <T> Path transform(
            Path bronzeRoot,
            Path silverPath,
            String outputFileName,
            String csvHeader,
            Function<T, String> csvFormatter,
            Class<T> recordType,
            java.util.function.BiFunction<String, T, String> dedupKey
    ) throws IOException {
        Files.createDirectories(silverPath);
        Path out = silverPath.resolve(outputFileName);

        StringBuilder csv = new StringBuilder();
        csv.append(csvHeader).append("\n");

        if (!Files.exists(bronzeRoot)) {
            atomicWrite(out, csv.toString());
            return out;
        }

        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        om.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Map<String, T> dedup = new LinkedHashMap<>();
        Files.walk(bronzeRoot)
            .filter(p -> p.getFileName().toString().equals("data.ndjson"))
            .forEach(p -> {
                try {
                    for (String line : Files.readAllLines(p)) {
                        if (line.isBlank()) continue;
                        T record = om.readValue(line, recordType);
                        String key = dedupKey.apply(null, record);
                        dedup.put(key, record);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

        for (T record : dedup.values()) {
            csv.append(csvFormatter.apply(record)).append("\n");
        }

        atomicWrite(out, csv.toString());
        log.info("Silver wrote {} rows to {}", dedup.size(), out);
        return out;
    }

    public Path copyCsvToParquet(Path csvPath, Path parquetPath) throws IOException {
        Files.createDirectories(parquetPath.getParent());
        Files.copy(csvPath, parquetPath, StandardCopyOption.REPLACE_EXISTING);
        return parquetPath;
    }

    private void atomicWrite(Path target, String content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, content);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
