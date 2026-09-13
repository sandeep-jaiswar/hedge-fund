package com.hedgefund.eia.ingest;

import com.hedgefund.eia.client.EiaClient;
import com.hedgefund.eia.config.EiaConfig;
import com.hedgefund.eia.store.EiaBronzeWriter;
import com.hedgefund.eia.store.EiaSilverTransformer;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class EiaIngestService extends AbstractIngestService {

    private final EiaConfig cfg;
    private final EiaClient client;
    private final EiaBronzeWriter bronzeWriter;
    private final EiaSilverTransformer silverTransformer;

    public EiaIngestService(EiaConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new EiaClient(cfg);
        this.bronzeWriter = new EiaBronzeWriter(bronzeRoot);
        this.silverTransformer = new EiaSilverTransformer();
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
        silverTransformer.transform(bronzeRoot, silverRoot, "eia.csv");
    }

    private String buildUrl(String key) {
        String base = cfg.baseUrl();
        return "https://www.eia.gov/dnav/pet/hist_xls/RBRTEd.xls";
    }
}
