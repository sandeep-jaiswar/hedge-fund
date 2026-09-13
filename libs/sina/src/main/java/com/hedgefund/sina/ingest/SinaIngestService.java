package com.hedgefund.sina.ingest;

import com.hedgefund.sina.client.SinaClient;
import com.hedgefund.sina.config.SinaConfig;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class SinaIngestService extends AbstractIngestService {

    private final SinaConfig cfg;
    private final SinaClient client;

    public SinaIngestService(SinaConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new SinaClient(cfg);
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
        silver.transformGeneric(bronzeRoot, silverRoot, "sina.csv");
    }

    private String buildUrl(String key) {
        String base = cfg.baseUrl();
        return base + "/list=" + key;
    }
}
