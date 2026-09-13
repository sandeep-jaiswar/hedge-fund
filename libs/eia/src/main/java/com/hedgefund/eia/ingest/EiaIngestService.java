package com.hedgefund.eia.ingest;

import com.hedgefund.eia.client.EiaClient;
import com.hedgefund.eia.config.EiaConfig;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class EiaIngestService extends AbstractIngestService {

    private final EiaConfig cfg;
    private final EiaClient client;

    public EiaIngestService(EiaConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new EiaClient(cfg);
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
        silver.transformGeneric(bronzeRoot, silverRoot, "eia.csv");
    }

    private String buildUrl(String key) {
        String base = cfg.baseUrl();
        return "https://www.eia.gov/dnav/pet/hist_xls/RBRTEd.xls";
    }
}
