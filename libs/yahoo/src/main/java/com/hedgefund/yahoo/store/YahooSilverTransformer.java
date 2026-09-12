package com.hedgefund.yahoo.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedgefund.yahoo.model.Bar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public class YahooSilverTransformer {

    private final ObjectMapper om = new ObjectMapper();

    public Path transform(Path bronzeRoot, Path silverPath) throws IOException {
        Files.createDirectories(silverPath);
        Path out = silverPath.resolve("yahoo_ohlcv.csv");

        StringBuilder csv = new StringBuilder();
        csv.append("symbol,date,epoch,open,high,low,close,adj_close,volume\n");

        if (!Files.exists(bronzeRoot)) {
            atomicWrite(out, csv.toString());
            return out;
        }

        Map<String, Bar> dedup = new LinkedHashMap<>();
        Files.walk(bronzeRoot)
            .filter(p -> p.getFileName().toString().equals("data.ndjson"))
            .forEach(p -> {
                try {
                    for (String line : Files.readAllLines(p)) {
                        if (line.isBlank()) continue;
                        Bar b = om.readValue(line, Bar.class);
                        dedup.put(b.symbol() + "|" + b.date(), b);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

        for (Bar b : dedup.values()) {
            csv.append(String.format("%s,%s,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%d%n",
                b.symbol(), b.date(), b.epoch(),
                b.open(), b.high(), b.low(), b.close(), b.adjClose(), b.volume()));
        }

        atomicWrite(out, csv.toString());
        return out;
    }

    private void atomicWrite(Path target, String content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, content);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
