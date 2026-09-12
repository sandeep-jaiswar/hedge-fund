package com.hedgefund.gmd.ingest;

import com.hedgefund.gmd.client.GmdClient;
import com.hedgefund.gmd.config.GmdConfig;
import com.hedgefund.gmd.store.GmdBronzeWriter;
import com.hedgefund.gmd.store.GmdSilverTransformer;
import com.hedgefund.observability.logging.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class GmdIngestService {

    private static final Logger log = LoggerFactory.getLogger(GmdIngestService.class);

    private final GmdConfig cfg;
    private final GmdClient client;
    private final GmdBronzeWriter bronze;
    private final GmdSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;

    public GmdIngestService(GmdConfig cfg, Path datalakeRoot) {
        this.cfg = cfg;
        this.client = new GmdClient(cfg);
        this.bronzeRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().bronze());
        this.silverRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().silver());
        this.bronze = new GmdBronzeWriter(bronzeRoot);
        this.silver = new GmdSilverTransformer();
    }

    public void run() throws Exception {
        CorrelationId.withContext("gmd");
        log.info("gmd ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());

        Files.createDirectories(bronzeRoot);
        Files.createDirectories(silverRoot);

        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            Semaphore sem = new Semaphore(cfg.ingestConfig().concurrency());
            List<Future<?>> futures = new ArrayList<>();

            for (String key : cfg.effectiveKeys()) {
                sem.acquire();
                futures.add(exec.submit(() -> {
                    CorrelationId.withContext("gmd");
                    try {
                        String url = buildUrl(key);
                        String raw = client.fetchRaw(url);
                        bronze.write(key, raw);
                        log.info("Done {} len={}", key, raw.length());
                    } catch (Exception e) {
                        log.error("Failed {}", key, e);
                        throw new RuntimeException(e);
                    } finally {
                        sem.release();
                        CorrelationId.clear();
                    }
                }));
            }

            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
        }

        Path out = silver.transform(bronzeRoot, silverRoot, "gmd.csv");
        log.info("Silver wrote {}", out);

        Files.writeString(bronzeRoot.resolve("_watermark.json"),
            "{\"lastRun\":\"" + java.time.Instant.now() + "\",\"keys\":" + cfg.effectiveKeys().size() + "}");

        CorrelationId.clear();
    }

    private String buildUrl(String key) {
        String base = cfg.baseUrl();
        return "https://raw.githubusercontent.com/datasets/s-and-p-500/master/data/data.csv";
    }
}
