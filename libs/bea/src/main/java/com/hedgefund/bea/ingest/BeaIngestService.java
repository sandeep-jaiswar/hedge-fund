package com.hedgefund.bea.ingest;

import com.hedgefund.bea.client.BeaClient;
import com.hedgefund.bea.config.BeaConfig;
import com.hedgefund.bea.store.BeaBronzeWriter;
import com.hedgefund.bea.store.BeaSilverTransformer;
import com.hedgefund.observability.logging.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BeaIngestService {

    private static final Logger log = LoggerFactory.getLogger(BeaIngestService.class);

    private final BeaConfig cfg;
    private final BeaClient client;
    private final BeaBronzeWriter bronze;
    private final BeaSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;

    public BeaIngestService(BeaConfig cfg, Path datalakeRoot) {
        this.cfg = cfg;
        this.client = new BeaClient(cfg);
        this.bronzeRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().bronze());
        this.silverRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().silver());
        this.bronze = new BeaBronzeWriter(bronzeRoot);
        this.silver = new BeaSilverTransformer();
    }

    public void run() throws Exception {
        CorrelationId.withContext("bea");
        log.info("bea ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());

        Files.createDirectories(bronzeRoot);
        Files.createDirectories(silverRoot);

        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            Semaphore sem = new Semaphore(cfg.ingestConfig().concurrency());
            List<Future<?>> futures = new ArrayList<>();

            for (String key : cfg.effectiveKeys()) {
                sem.acquire();
                futures.add(exec.submit(() -> {
                    CorrelationId.withContext("bea");
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

        Path out = silver.transform(bronzeRoot, silverRoot, "bea.csv");
        log.info("Silver wrote {}", out);

        Files.writeString(bronzeRoot.resolve("_watermark.json"),
            "{\"lastRun\":\"" + java.time.Instant.now() + "\",\"keys\":" + cfg.effectiveKeys().size() + "}");

        CorrelationId.clear();
    }

    private String buildUrl(String key) {
        String base = cfg.baseUrl();
        return "https://apps.bea.gov/api/data?UserID=demo&method=GetData&DataSetName=NIPA&TableName=T10101&Frequency=Q&Year=2023";
    }
}
