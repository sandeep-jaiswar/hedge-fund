package com.hedgefund.yahoo.ingest;

import com.hedgefund.observability.logging.CorrelationId;
import com.hedgefund.yahoo.client.YahooClient;
import com.hedgefund.yahoo.config.YahooConfig;
import com.hedgefund.yahoo.model.Bar;
import com.hedgefund.yahoo.store.YahooBronzeWriter;
import com.hedgefund.yahoo.store.YahooSilverTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class YahooIngestService {

    private static final Logger log = LoggerFactory.getLogger(YahooIngestService.class);

    private final YahooConfig cfg;
    private final YahooClient client;
    private final YahooBronzeWriter bronze;
    private final YahooSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;

    public YahooIngestService(YahooConfig cfg, Path datalakeRoot) {
        this.cfg = cfg;
        this.client = new YahooClient(cfg);
        this.bronzeRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().bronze());
        this.silverRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().silver());
        this.bronze = new YahooBronzeWriter(bronzeRoot);
        this.silver = new YahooSilverTransformer();
    }

    public void run() throws Exception {
        CorrelationId.withContext("yahoo");
        log.info("Yahoo ingest start symbols={} interval={} range={}", cfg.symbols(), cfg.interval(), cfg.range());

        Files.createDirectories(bronzeRoot);
        Files.createDirectories(silverRoot);

        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            Semaphore sem = new Semaphore(cfg.ingestConfig().concurrency());
            List<Future<?>> futures = new ArrayList<>();

            for (String sym : cfg.symbols()) {
                sem.acquire();
                futures.add(exec.submit(() -> {
                    CorrelationId.withContext("yahoo");
                    try {
                        List<Bar> bars = client.fetchChart(sym);
                        bronze.write(sym, bars, null);
                        log.info("Done {}", sym);
                    } catch (Exception e) {
                        log.error("Failed {}", sym, e);
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

        Path out = silver.transform(bronzeRoot, silverRoot);
        long rows = Files.lines(out).count() - 1;
        log.info("Silver wrote {} rows to {}", rows, out);

        Files.writeString(bronzeRoot.resolve("_watermark.json"),
            "{\"lastRun\":\"" + java.time.Instant.now() + "\",\"symbols\":" + cfg.symbols().size() + "}");

        CorrelationId.clear();
    }
}
