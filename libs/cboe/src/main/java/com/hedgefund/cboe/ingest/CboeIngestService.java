package com.hedgefund.cboe.ingest;

import com.hedgefund.cboe.client.CboeClient;
import com.hedgefund.cboe.config.CboeConfig;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class CboeIngestService extends AbstractIngestService {

    private final CboeConfig cfg;
    private final CboeClient client;

    public CboeIngestService(CboeConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new CboeClient(cfg);
    }

    @Override
    protected List<String> keys() {
        return cfg.symbols();
    }

    @Override
    protected void ingestSymbol(String symbol) throws Exception {
        bronze.writeRawJson(symbol, client.fetchRaw(buildUrl(symbol)));
    }

    @Override
    protected void transformSilver() throws Exception {
        silver.transformGeneric(bronzeRoot, silverRoot, "cboe.csv");
    }

    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return "https://query1.finance.yahoo.com/v8/finance/chart/%5EVIX?interval=1d&range=10y";
    }
}
