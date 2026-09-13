package com.hedgefund.sec.ingest;

import com.hedgefund.sec.client.SecClient;
import com.hedgefund.sec.config.SecConfig;
import com.hedgefund.sec.store.SecBronzeWriter;
import com.hedgefund.sec.store.SecSilverTransformer;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class SecIngestService extends AbstractIngestService {

    private final SecConfig cfg;
    private final SecClient client;
    private final SecBronzeWriter bronzeWriter;
    private final SecSilverTransformer silverTransformer;

    public SecIngestService(SecConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new SecClient(cfg);
        this.bronzeWriter = new SecBronzeWriter(bronzeRoot);
        this.silverTransformer = new SecSilverTransformer();
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
        silverTransformer.transform(bronzeRoot, silverRoot, "sec.csv");
    }

    private String buildUrl(String key) {
        String base = cfg.baseUrl();
        return "https://data.sec.gov/submissions/CIK0000320193.json";
    }
}
