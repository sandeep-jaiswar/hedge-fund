package com.hedgefund.calcfi.ingest;

import com.hedgefund.calcfi.client.CalcfiClient;
import com.hedgefund.calcfi.config.CalcfiConfig;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class CalcfiIngestService extends AbstractIngestService {

    private final CalcfiConfig cfg;
    private final CalcfiClient client;

    public CalcfiIngestService(CalcfiConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new CalcfiClient(cfg);
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
        silver.transformGeneric(bronzeRoot, silverRoot, "calcfi.csv");
    }

    private String buildUrl(String key) {
        // Use CalcFi native API for S&P 500 data
        return "https://api.calcfi.com/api/v1/series/sp500";
    }
}
