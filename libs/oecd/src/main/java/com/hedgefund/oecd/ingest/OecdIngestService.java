package com.hedgefund.oecd.ingest;

import com.hedgefund.oecd.client.OecdClient;
import com.hedgefund.oecd.config.OecdConfig;
import com.hedgefund.oecd.store.OecdBronzeWriter;
import com.hedgefund.oecd.store.OecdSilverTransformer;
import com.hedgefund.observability.logging.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class OecdIngestService {

    private static final Logger log = LoggerFactory.getLogger(OecdIngestService.class);

    private final OecdConfig cfg;
    private final OecdClient client;
    private final OecdBronzeWriter bronze;
    private final OecdSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;

    public OecdIngestService(OecdConfig cfg, Path datalakeRoot) {
        this.cfg = cfg;
        this.client = new OecdClient(cfg);
        this.bronzeRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().bronze());
        this.silverRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().silver());
        this.bronze = new OecdBronzeWriter(bronzeRoot);
        this.silver = new OecdSilverTransformer();
    }

    public void run() throws Exception {
        CorrelationId.withContext("oecd");
        log.info("oecd ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());

        Files.createDirectories(bronzeRoot);
        Files.createDirectories(silverRoot);

        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            Semaphore sem = new Semaphore(cfg.ingestConfig().concurrency());
            List<Future<?>> futures = new ArrayList<>();

            for (String key : cfg.effectiveKeys()) {
                sem.acquire();
                futures.add(exec.submit(() -> {
                    CorrelationId.withContext("oecd");
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

        Path out = silver.transform(bronzeRoot, silverRoot, "oecd.csv");
        log.info("Silver wrote {}", out);

        Files.writeString(bronzeRoot.resolve("_watermark.json"),
            "{\"lastRun\":\"" + java.time.Instant.now() + "\",\"keys\":" + cfg.effectiveKeys().size() + "}");

        CorrelationId.clear();
    }

    private String buildUrl(String key) {
        String base = cfg.baseUrl();
        return "https://sdmx.oecd.org/public/rest/data/OECD.SDD.STES,DSD_KEI@DF_KEI,4.0/USA.CP";
    }
}
