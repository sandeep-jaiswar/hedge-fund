package com.hedgefund.bls.ingest;

import com.hedgefund.bls.client.BlsClient;
import com.hedgefund.bls.config.BlsConfig;
import com.hedgefund.bls.store.BlsBronzeWriter;
import com.hedgefund.bls.store.BlsSilverTransformer;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class BlsIngestService extends AbstractIngestService {

    private final BlsConfig cfg;
    private final BlsClient client;
    private final BlsBronzeWriter bronzeWriter;
    private final BlsSilverTransformer silverTransformer;

    public BlsIngestService(BlsConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new BlsClient(cfg);
        this.bronzeWriter = new BlsBronzeWriter(bronzeRoot);
        this.silverTransformer = new BlsSilverTransformer();
    }

    @Override
    protected List<String> keys() {
        return cfg.effectiveKeys();
    }

    @Override
    protected void ingestSymbol(String symbol) throws Exception {
        // Use BLS API v2 with POST request for batch series fetch
        bronzeWriter.write(symbol, client.fetchBlsSeries(List.of(symbol)));
    }

    @Override
    protected void transformSilver() throws Exception {
        silverTransformer.transform(bronzeRoot, silverRoot, "bls.csv");
    }
}
