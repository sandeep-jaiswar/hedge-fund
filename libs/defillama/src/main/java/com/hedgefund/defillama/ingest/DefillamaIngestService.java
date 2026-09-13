package com.hedgefund.defillama.ingest;

import com.hedgefund.defillama.client.DefillamaClient;
import com.hedgefund.defillama.config.DefillamaConfig;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class DefillamaIngestService extends AbstractIngestService {

    private final DefillamaConfig cfg;
    private final DefillamaClient client;

    public DefillamaIngestService(DefillamaConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new DefillamaClient(cfg);
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
        silver.transformGeneric(bronzeRoot, silverRoot, "defillama.csv");
    }

    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return base+"/protocol/"+key;
    }
}
