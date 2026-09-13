package com.hedgefund.fred.ingest;

import com.hedgefund.fred.client.FredClient;
import com.hedgefund.fred.config.FredConfig;
import com.hedgefund.fred.store.FredBronzeWriter;
import com.hedgefund.fred.store.FredSilverTransformer;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class FredIngestService extends AbstractIngestService {

    private final FredConfig cfg;
    private final FredClient client;
    private final FredBronzeWriter bronzeWriter;
    private final FredSilverTransformer silverTransformer;

    public FredIngestService(FredConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new FredClient(cfg);
        this.bronzeWriter = new FredBronzeWriter(bronzeRoot);
        this.silverTransformer = new FredSilverTransformer();
    }

    @Override
    protected List<String> keys() {
        return cfg.effectiveKeys();
    }

    @Override
    protected void ingestSymbol(String symbol) throws Exception {
        bronzeWriter.write(symbol, client.fetchRaw(buildUrl(symbol)));
    }

    @Override
    protected void transformSilver() throws Exception {
        silverTransformer.transform(bronzeRoot, silverRoot, "fred.csv");
    }

    private String buildUrl(String key) {
        String url = cfg.baseUrl() + "/api/fred/series/observations?series_id=" + key + "&limit=" + cfg.limit();
        if (cfg.apiKey() != null && !cfg.apiKey().isEmpty()) {
            url += "&api_key=" + cfg.apiKey();
        }
        return url;
    }
}
