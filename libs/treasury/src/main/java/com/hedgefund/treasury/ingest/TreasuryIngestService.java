package com.hedgefund.treasury.ingest;

import com.hedgefund.treasury.client.TreasuryClient;
import com.hedgefund.treasury.config.TreasuryConfig;
import com.hedgefund.treasury.store.TreasuryBronzeWriter;
import com.hedgefund.treasury.store.TreasurySilverTransformer;
import com.hedgefund.observability.logging.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class TreasuryIngestService {

    private static final Logger log = LoggerFactory.getLogger(TreasuryIngestService.class);

    private final TreasuryConfig cfg;
    private final TreasuryClient client;
    private final TreasuryBronzeWriter bronze;
    private final TreasurySilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;

    public TreasuryIngestService(TreasuryConfig cfg, Path datalakeRoot) {
        this.cfg = cfg;
        this.client = new TreasuryClient(cfg);
        this.bronzeRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().bronze());
        this.silverRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().silver());
        this.bronze = new TreasuryBronzeWriter(bronzeRoot);
        this.silver = new TreasurySilverTransformer();
    }

    public void run() throws Exception {
        CorrelationId.withContext("treasury");
        log.info("treasury ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());

        Files.createDirectories(bronzeRoot);
        Files.createDirectories(silverRoot);

        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            Semaphore sem = new Semaphore(cfg.ingestConfig().concurrency());
            List<Future<?>> futures = new ArrayList<>();

            for (String key : cfg.effectiveKeys()) {
                sem.acquire();
                futures.add(exec.submit(() -> {
                    CorrelationId.withContext("treasury");
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

        Path out = silver.transform(bronzeRoot, silverRoot, "treasury.csv");
        log.info("Silver wrote {}", out);

        Files.writeString(bronzeRoot.resolve("_watermark.json"),
            "{\"lastRun\":\"" + java.time.Instant.now() + "\",\"keys\":" + cfg.effectiveKeys().size() + "}");

        CorrelationId.clear();
    }

    private String buildUrl(String key) {
        return cfg.baseUrl() + "/resource-center/data-chart-center/interest-rates/daily-treasury-rates.csv/2024/all?type=daily_treasury_yield_curve";
    }
}
