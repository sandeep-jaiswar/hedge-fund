package com.hedgefund.gmd.ingest;

import com.hedgefund.gmd.client.GmdClient;
import com.hedgefund.gmd.config.GmdConfig;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class GmdIngestService extends AbstractIngestService {

    private final GmdConfig cfg;
    private final GmdClient client;

    public GmdIngestService(GmdConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new GmdClient(cfg);
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
        silver.transformGeneric(bronzeRoot, silverRoot, "gmd.csv");
    }

    private String buildUrl(String key) {
        // Use GMD native API for macroeconomic data
        // GitHub raw content: https://raw.githubusercontent.com/GlobalMacroDatabase/GMD/main/Datasets/
        return "https://raw.githubusercontent.com/GlobalMacroDatabase/GMD/main/Datasets/AFG.csv";
    }
}
