package com.hedgefund.binance.ingest;

import com.hedgefund.binance.client.BinanceClient;
import com.hedgefund.binance.config.BinanceConfig;
import com.hedgefund.binance.store.BinanceBronzeWriter;
import com.hedgefund.binance.store.BinanceSilverTransformer;
import com.hedgefund.observability.logging.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BinanceIngestService {

    private static final Logger log = LoggerFactory.getLogger(BinanceIngestService.class);

    private final BinanceConfig cfg;
    private final BinanceClient client;
    private final BinanceBronzeWriter bronze;
    private final BinanceSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;

    public BinanceIngestService(BinanceConfig cfg, Path datalakeRoot) {
        this.cfg = cfg;
        this.client = new BinanceClient(cfg);
        this.bronzeRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().bronze());
        this.silverRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().silver());
        this.bronze = new BinanceBronzeWriter(bronzeRoot);
        this.silver = new BinanceSilverTransformer();
    }

    public void run() throws Exception {
        CorrelationId.withContext("binance");
        log.info("binance ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());

        Files.createDirectories(bronzeRoot);
        Files.createDirectories(silverRoot);

        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            Semaphore sem = new Semaphore(cfg.ingestConfig().concurrency());
            List<Future<?>> futures = new ArrayList<>();

            for (String key : cfg.effectiveKeys()) {
                sem.acquire();
                futures.add(exec.submit(() -> {
                    CorrelationId.withContext("binance");
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

        Path out = silver.transform(bronzeRoot, silverRoot, "binance.csv");
        log.info("Silver wrote {}", out);

        Files.writeString(bronzeRoot.resolve("_watermark.json"),
            "{\"lastRun\":\"" + java.time.Instant.now() + "\",\"keys\":" + cfg.effectiveKeys().size() + "}");

        CorrelationId.clear();
    }

    private String buildUrl(String key) {
        return cfg.baseUrl() + "/api/v3/klines?symbol=" + key + "&interval=" + cfg.interval() + "&limit=" + cfg.limit();
    }
}
