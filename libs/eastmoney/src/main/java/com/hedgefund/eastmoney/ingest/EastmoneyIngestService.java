package com.hedgefund.eastmoney.ingest;

import com.hedgefund.eastmoney.client.EastmoneyClient;
import com.hedgefund.eastmoney.config.EastmoneyConfig;
import com.hedgefund.eastmoney.store.EastmoneyBronzeWriter;
import com.hedgefund.eastmoney.store.EastmoneySilverTransformer;
import com.hedgefund.observability.logging.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class EastmoneyIngestService {

    private static final Logger log = LoggerFactory.getLogger(EastmoneyIngestService.class);

    private final EastmoneyConfig cfg;
    private final EastmoneyClient client;
    private final EastmoneyBronzeWriter bronze;
    private final EastmoneySilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;

    public EastmoneyIngestService(EastmoneyConfig cfg, Path datalakeRoot) {
        this.cfg = cfg;
        this.client = new EastmoneyClient(cfg);
        this.bronzeRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().bronze());
        this.silverRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().silver());
        this.bronze = new EastmoneyBronzeWriter(bronzeRoot);
        this.silver = new EastmoneySilverTransformer();
    }

    public void run() throws Exception {
        CorrelationId.withContext("eastmoney");
        log.info("eastmoney ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());

        Files.createDirectories(bronzeRoot);
        Files.createDirectories(silverRoot);

        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            Semaphore sem = new Semaphore(cfg.ingestConfig().concurrency());
            List<Future<?>> futures = new ArrayList<>();

            for (String key : cfg.effectiveKeys()) {
                sem.acquire();
                futures.add(exec.submit(() -> {
                    CorrelationId.withContext("eastmoney");
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

        Path out = silver.transform(bronzeRoot, silverRoot, "eastmoney.csv");
        log.info("Silver wrote {}", out);

        Files.writeString(bronzeRoot.resolve("_watermark.json"),
            "{\"lastRun\":\"" + java.time.Instant.now() + "\",\"keys\":" + cfg.effectiveKeys().size() + "}");

        CorrelationId.clear();
    }

    private String buildUrl(String key) {
        String base = cfg.baseUrl();
        return "https://qt.gtimg.cn/q=sh600000";
    }
}
