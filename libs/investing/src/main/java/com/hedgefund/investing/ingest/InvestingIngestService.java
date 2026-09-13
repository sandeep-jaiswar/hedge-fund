package com.hedgefund.investing.ingest;

import com.hedgefund.investing.client.InvestingClient;
import com.hedgefund.investing.config.InvestingConfig;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class InvestingIngestService extends AbstractIngestService {

    private final InvestingConfig cfg;
    private final InvestingClient client;

    public InvestingIngestService(InvestingConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new InvestingClient(cfg);
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
        silver.transformGeneric(bronzeRoot, silverRoot, "investing.csv");
    }

    private String buildUrl(String key) {
        String base = cfg.baseUrl();
        return "https://query1.finance.yahoo.com/v8/finance/chart/" + key + "?interval=1d&range=10y";
    }
}
