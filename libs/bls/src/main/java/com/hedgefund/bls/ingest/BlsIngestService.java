package com.hedgefund.bls.ingest;

import com.hedgefund.bls.client.BlsClient;
import com.hedgefund.bls.config.BlsConfig;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class BlsIngestService extends AbstractIngestService {

    private final BlsConfig cfg;
    private final BlsClient client;

    public BlsIngestService(BlsConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new BlsClient(cfg);
    }

    @Override
    protected List<String> keys() {
        return cfg.effectiveKeys();
    }

    @Override
    protected void ingestSymbol(String symbol) throws Exception {
        // Use BLS API v2 with POST request for batch series fetch
        bronze.writeRawJson(symbol, client.fetchBlsSeries(List.of(symbol)));
    }

    @Override
    protected void transformSilver() throws Exception {
        silver.transformGeneric(bronzeRoot, silverRoot, "bls.csv");
    }
}
