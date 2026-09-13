package com.hedgefund.fdic.ingest;

import com.hedgefund.fdic.client.FdicClient;
import com.hedgefund.fdic.config.FdicConfig;
import com.hedgefund.fdic.store.FdicBronzeWriter;
import com.hedgefund.fdic.store.FdicSilverTransformer;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class FdicIngestService extends AbstractIngestService {

    private final FdicConfig cfg;
    private final FdicClient client;
    private final FdicBronzeWriter bronzeWriter;
    private final FdicSilverTransformer silverTransformer;

    public FdicIngestService(FdicConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new FdicClient(cfg);
        this.bronzeWriter = new FdicBronzeWriter(bronzeRoot);
        this.silverTransformer = new FdicSilverTransformer();
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
        silverTransformer.transform(bronzeRoot, silverRoot, "fdic.csv");
    }

    private String buildUrl(String key) {
        // Use FDIC native API for bank rates data
        // Endpoint: https://www.fdic.gov/resources/bankers/national-rates/
        return "https://www.fdic.gov/resources/bankers/national-rates/data/national-rates.csv";
    }
}
