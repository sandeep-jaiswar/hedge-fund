package com.hedgefund.fdic.ingest;

import com.hedgefund.fdic.client.FdicClient;
import com.hedgefund.fdic.config.FdicConfig;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class FdicIngestService extends AbstractIngestService {

    private final FdicConfig cfg;
    private final FdicClient client;

    public FdicIngestService(FdicConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new FdicClient(cfg);
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
        silver.transformGeneric(bronzeRoot, silverRoot, "fdic.csv");
    }

    private String buildUrl(String key) {
        // Use FDIC native API for bank rates data
        // Endpoint: https://www.fdic.gov/resources/bankers/national-rates/
        return "https://www.fdic.gov/resources/bankers/national-rates/data/national-rates.csv";
    }
}
