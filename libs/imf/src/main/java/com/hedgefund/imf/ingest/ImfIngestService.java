package com.hedgefund.imf.ingest;

import com.hedgefund.imf.client.ImfClient;
import com.hedgefund.imf.config.ImfConfig;
import com.hedgefund.imf.store.ImfBronzeWriter;
import com.hedgefund.imf.store.ImfSilverTransformer;
import com.hedgefund.observability.logging.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ImfIngestService {

    private static final Logger log = LoggerFactory.getLogger(ImfIngestService.class);

    private final ImfConfig cfg;
    private final ImfClient client;
    private final ImfBronzeWriter bronze;
    private final ImfSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;

    public ImfIngestService(ImfConfig cfg, Path datalakeRoot) {
        this.cfg = cfg;
        this.client = new ImfClient(cfg);
        this.bronzeRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().bronze());
        this.silverRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().silver());
        this.bronze = new ImfBronzeWriter(bronzeRoot);
        this.silver = new ImfSilverTransformer();
    }

    public void run() throws Exception {
        CorrelationId.withContext("imf");
        log.info("imf ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());

        Files.createDirectories(bronzeRoot);
        Files.createDirectories(silverRoot);

        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            Semaphore sem = new Semaphore(cfg.ingestConfig().concurrency());
            List<Future<?>> futures = new ArrayList<>();

            for (String key : cfg.effectiveKeys()) {
                sem.acquire();
                futures.add(exec.submit(() -> {
                    CorrelationId.withContext("imf");
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

        Path out = silver.transform(bronzeRoot, silverRoot, "imf.csv");
        log.info("Silver wrote {}", out);

        Files.writeString(bronzeRoot.resolve("_watermark.json"),
            "{\"lastRun\":\"" + java.time.Instant.now() + "\",\"keys\":" + cfg.effectiveKeys().size() + "}");

        CorrelationId.clear();
    }

    private String buildUrl(String key) {
        String base = cfg.baseUrl();
        return "https://www.imf.org/external/datamapper/api/NGDP_RPCH?periods=2023";
    }
}
