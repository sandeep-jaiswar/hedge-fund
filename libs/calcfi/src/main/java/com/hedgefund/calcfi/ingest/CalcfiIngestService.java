package com.hedgefund.calcfi.ingest;

import com.hedgefund.calcfi.client.CalcfiClient;
import com.hedgefund.calcfi.config.CalcfiConfig;
import com.hedgefund.calcfi.store.CalcfiBronzeWriter;
import com.hedgefund.calcfi.store.CalcfiSilverTransformer;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class CalcfiIngestService extends AbstractIngestService {

    private final CalcfiConfig cfg;
    private final CalcfiClient client;
    private final CalcfiBronzeWriter bronzeWriter;
    private final CalcfiSilverTransformer silverTransformer;

    public CalcfiIngestService(CalcfiConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new CalcfiClient(cfg);
        this.bronzeWriter = new CalcfiBronzeWriter(bronzeRoot);
        this.silverTransformer = new CalcfiSilverTransformer();
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
        silverTransformer.transform(bronzeRoot, silverRoot, "calcfi.csv");
    }

    private String buildUrl(String key) {
        // Use CalcFi native API for S&P 500 data
        return "https://api.calcfi.com/api/v1/series/sp500";
    }
}
