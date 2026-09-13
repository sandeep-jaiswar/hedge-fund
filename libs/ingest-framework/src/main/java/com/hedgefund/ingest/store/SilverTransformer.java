package com.hedgefund.ingest.store;

import com.hedgefund.common.FileUtils;
import com.hedgefund.common.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
            FileUtils.atomicWrite(out, csv.toString());
            return out;
        }

        Map<String, T> dedup = new LinkedHashMap<>();
        Files.walk(bronzeRoot)
            .filter(p -> p.getFileName().toString().equals("data.ndjson"))
            .forEach(p -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(p)))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.isBlank()) continue;
                        T record = Json.shared().readValue(line, recordType);
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

        FileUtils.atomicWrite(out, csv.toString());
        log.info("Silver wrote {} rows to {}", dedup.size(), out);
        return out;
    }

    public Path copyCsvToParquet(Path csvPath, Path parquetPath) throws IOException {
        Files.createDirectories(parquetPath.getParent());
        Files.copy(csvPath, parquetPath, StandardCopyOption.REPLACE_EXISTING);
        return parquetPath;
    }

    /**
     * Generic passthrough silver: one row per bronze data file
     * (source_key,raw_len,bronze_path). Replaces the 17 identical
     * per-source *SilverTransformer copies.
     */
    public Path transformGeneric(Path bronzeRoot, Path silverPath, String outputFileName) throws IOException {
        Files.createDirectories(silverPath);
        Path out = silverPath.resolve(outputFileName);

        StringBuilder csv = new StringBuilder();
        csv.append("source_key,raw_len,bronze_path\n");

        if (Files.exists(bronzeRoot)) {
            try (var stream = Files.walk(bronzeRoot)) {
                stream.filter(p -> p.getFileName().toString().startsWith("data.")).forEach(p -> {
                    try {
                        String key = p.getParent().getFileName().toString().replace("key=", "");
                        long len = Files.size(p);
                        csv.append(key).append(",").append(len).append(",")
                            .append(p.toString().replace(",", "_")).append("\n");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        FileUtils.atomicWrite(out, csv.toString());
        log.info("Silver generic wrote {}", out);
        return out;
    }
}
