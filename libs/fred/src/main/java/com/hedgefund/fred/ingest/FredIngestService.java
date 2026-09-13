package com.hedgefund.fred.ingest;

import com.hedgefund.fred.client.FredClient;
import com.hedgefund.fred.config.FredConfig;
import com.hedgefund.ingest.service.AbstractIngestService;

import java.nio.file.Path;
import java.util.List;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class FredIngestService extends AbstractIngestService {

    private final FredConfig cfg;
    private final FredClient client;

    public FredIngestService(FredConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new FredClient(cfg);
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
        silver.transformGeneric(bronzeRoot, silverRoot, "fred.csv");
    }

    private String buildUrl(String key) {
        String url = cfg.baseUrl() + "/api/fred/series/observations?series_id=" + key + "&limit=" + cfg.limit();
        if (cfg.apiKey() != null && !cfg.apiKey().isEmpty()) {
            url += "&api_key=" + cfg.apiKey();
        }
        return url;
    }
}
