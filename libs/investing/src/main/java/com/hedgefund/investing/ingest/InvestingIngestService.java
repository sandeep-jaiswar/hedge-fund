package com.hedgefund.investing.ingest;

import com.hedgefund.investing.client.InvestingClient;
import com.hedgefund.investing.config.InvestingConfig;
import com.hedgefund.investing.store.InvestingBronzeWriter;
import com.hedgefund.investing.store.InvestingSilverTransformer;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class InvestingIngestService extends AbstractIngestService {

    private final InvestingConfig cfg;
    private final InvestingClient client;
    private final InvestingBronzeWriter bronzeWriter;
    private final InvestingSilverTransformer silverTransformer;

    public InvestingIngestService(InvestingConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new InvestingClient(cfg);
        this.bronzeWriter = new InvestingBronzeWriter(bronzeRoot);
        this.silverTransformer = new InvestingSilverTransformer();
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
        silverTransformer.transform(bronzeRoot, silverRoot, "investing.csv");
    }

    private String buildUrl(String key) {
        String base = cfg.baseUrl();
        return "https://query1.finance.yahoo.com/v8/finance/chart/" + key + "?interval=1d&range=10y";
    }
}
