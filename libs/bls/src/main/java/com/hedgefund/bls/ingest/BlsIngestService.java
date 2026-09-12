package com.hedgefund.bls.ingest;

import com.hedgefund.bls.client.BlsClient;
import com.hedgefund.bls.config.BlsConfig;
import com.hedgefund.bls.store.BlsBronzeWriter;
import com.hedgefund.bls.store.BlsSilverTransformer;
import com.hedgefund.observability.logging.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BlsIngestService {
    private static final Logger log = LoggerFactory.getLogger(BlsIngestService.class);
    private final BlsConfig cfg;
    private final BlsClient client;
    private final BlsBronzeWriter bronze;
    private final BlsSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;

    public BlsIngestService(BlsConfig cfg, Path datalakeRoot) {
        this.cfg = cfg;
        this.client = new BlsClient(cfg);
        this.bronzeRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().bronze());
        this.silverRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().silver());
        this.bronze = new BlsBronzeWriter(bronzeRoot);
        this.silver = new BlsSilverTransformer();
    }

    public void run() throws Exception {
        CorrelationId.withContext("bls");
        try {
            log.info("bls ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());
            Files.createDirectories(bronzeRoot);
            Files.createDirectories(silverRoot);
            try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
                Semaphore sem = new Semaphore(cfg.ingestConfig().concurrency());
                List<Future<?>> futures = new ArrayList<>();
                for (String key : cfg.effectiveKeys()) {
                    sem.acquire();
                    futures.add(exec.submit(() -> {
                        try {
                            CorrelationId.withContext("bls");
                            String url = buildUrl(key);
                            String raw = client.fetchRaw(url);
                            bronze.write(key, raw);
                            log.info("Done {} len={}", key, raw.length());
                        } catch (Exception e) {
                            log.error("Failed {}", key, e);
                            throw new RuntimeException(e);
                        } finally {
                            CorrelationId.clear();
                            sem.release();
                        }
                    }));
                }
                for (Future<?> f : futures) f.get(60, TimeUnit.SECONDS);
            }
            Path out = silver.transform(bronzeRoot, silverRoot, "bls.csv");
            log.info("Silver wrote {}", out);
            Files.writeString(bronzeRoot.resolve("_watermark.json"), "{\"lastRun\":\"" + java.time.Instant.now().toString() + "\",\"keys\":" + cfg.effectiveKeys().size() + "}");
        } finally {
            CorrelationId.clear();
        }
    }

    private String buildUrl(String key) {
        String base = cfg.baseUrl();
        return "https://download.bls.gov/pub/time.series/ap/ap.data.0.Current";
    }
}
