package com.hedgefund.cboe.ingest;

import com.hedgefund.cboe.client.CboeClient;
import com.hedgefund.cboe.config.CboeConfig;
import com.hedgefund.cboe.store.CboeBronzeWriter;
import com.hedgefund.cboe.store.CboeSilverTransformer;
import java.nio.file.*;
import java.util.*;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class CboeIngestService extends AbstractIngestService {

    private final CboeConfig cfg;
    private final CboeClient client;
    private final CboeBronzeWriter bronzeWriter;
    private final CboeSilverTransformer silverTransformer;

    public CboeIngestService(CboeConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new CboeClient(cfg);
        this.bronzeWriter = new CboeBronzeWriter(bronzeRoot);
        this.silverTransformer = new CboeSilverTransformer();
    }

    @Override
    protected List<String> keys() {
        return cfg.symbols();
    }

    @Override
    protected void ingestSymbol(String symbol) throws Exception {
        bronzeWriter.write(symbol, client.fetchRaw(buildUrl(symbol)));
    }

    @Override
    protected void transformSilver() throws Exception {
        silverTransformer.transform(bronzeRoot, silverRoot, "cboe.csv");
    }

    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return "https://query1.finance.yahoo.com/v8/finance/chart/%5EVIX?interval=1d&range=10y";
    }
}
