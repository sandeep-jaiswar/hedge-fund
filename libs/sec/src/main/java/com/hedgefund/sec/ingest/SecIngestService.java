package com.hedgefund.sec.ingest;

import com.hedgefund.sec.client.SecClient;
import com.hedgefund.sec.config.SecConfig;
import com.hedgefund.ingest.service.AbstractIngestService;

import java.nio.file.Path;
import java.util.List;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class SecIngestService extends AbstractIngestService {

    private final SecConfig cfg;
    private final SecClient client;

    public SecIngestService(SecConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new SecClient(cfg);
    }

    @Override
    protected List<String> keys() {
        return cfg.effectiveKeys();
    }

    @Override
    protected void ingestSymbol(String symbol) throws Exception {
        bronze.writeRawJson(symbol, client.fetchRaw(buildUrl(symbol)));
    }

    @Override
    protected void transformSilver() throws Exception {
        silver.transformGeneric(bronzeRoot, silverRoot, "sec.csv");
    }

    private String buildUrl(String key) {
        return "https://data.sec.gov/submissions/CIK0000320193.json";
    }
}
