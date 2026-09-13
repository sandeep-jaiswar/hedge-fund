package com.hedgefund.ingest.service;

import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.store.BronzeWriter;
import com.hedgefund.ingest.store.SilverTransformer;
import com.hedgefund.datalake.Datalake;
import com.hedgefund.observability.logging.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public abstract class AbstractIngestService {

    private static final Logger log = LoggerFactory.getLogger(AbstractIngestService.class);

    protected final IngestConfig config;
    protected final Path bronzeRoot;
    protected final Path silverRoot;
    protected final BronzeWriter bronze;
    protected final SilverTransformer silver;

    protected AbstractIngestService(IngestConfig config, Path datalakeRoot) {
        this.config = config;
        this.bronzeRoot = datalakeRoot.resolve(config.paths().bronze());
        this.silverRoot = datalakeRoot.resolve(config.paths().silver());
        this.bronze = new BronzeWriter(bronzeRoot);
        this.silver = new SilverTransformer();
    }

    public void run() throws Exception {
        CorrelationId.withContext(config.sourceId());
        try {
            List<String> keys = keys();
            log.info("Ingest start: source={} symbols={}", config.sourceId(), keys.size());
            Files.createDirectories(bronzeRoot);
            Files.createDirectories(silverRoot);

            try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
                Semaphore sem = new Semaphore(config.concurrency());
                List<Future<?>> futures = new ArrayList<>();

                for (String symbol : keys) {
                    sem.acquire();
                    futures.add(exec.submit(() -> {
                        CorrelationId.withContext(config.sourceId());
                        try {
                            ingestSymbol(symbol);
                            log.info("Done: {} [{}]", config.sourceId(), symbol);
                        } catch (Exception e) {
                            log.error("Failed: {} [{}]", config.sourceId(), symbol, e);
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

            transformSilver();
            bronze.writeWatermark(keys.size());
            log.info("Ingest complete: source={}", config.sourceId());
        } finally {
            CorrelationId.clear();
        }
    }

    /** Keys to fetch. Defaults to config.symbols(); override for alias keys (series/tickers/protocols). */
    protected List<String> keys() {
        return config.symbols();
    }

    protected abstract void ingestSymbol(String symbol) throws Exception;

    protected abstract void transformSilver() throws Exception;

    protected Path getBronzeDir(String symbol) {
        return bronzeRoot.resolve("symbol=" + symbol);
    }
}
