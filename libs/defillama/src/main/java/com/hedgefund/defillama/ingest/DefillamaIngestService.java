package com.hedgefund.defillama.ingest;

import com.hedgefund.defillama.client.DefillamaClient;
import com.hedgefund.defillama.config.DefillamaConfig;
import com.hedgefund.defillama.store.DefillamaBronzeWriter;
import com.hedgefund.defillama.store.DefillamaSilverTransformer;
import java.nio.file.*;
import java.util.*;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class DefillamaIngestService extends AbstractIngestService {

    private final DefillamaConfig cfg;
    private final DefillamaClient client;
    private final DefillamaBronzeWriter bronzeWriter;
    private final DefillamaSilverTransformer silverTransformer;

    public DefillamaIngestService(DefillamaConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new DefillamaClient(cfg);
        this.bronzeWriter = new DefillamaBronzeWriter(bronzeRoot);
        this.silverTransformer = new DefillamaSilverTransformer();
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
        silverTransformer.transform(bronzeRoot, silverRoot, "defillama.csv");
    }

    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return base+"/protocol/"+key;
    }
}
