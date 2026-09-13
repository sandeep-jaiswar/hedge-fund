package com.hedgefund.gmd.ingest;

import com.hedgefund.gmd.client.GmdClient;
import com.hedgefund.gmd.config.GmdConfig;
import com.hedgefund.gmd.store.GmdBronzeWriter;
import com.hedgefund.gmd.store.GmdSilverTransformer;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class GmdIngestService extends AbstractIngestService {

    private final GmdConfig cfg;
    private final GmdClient client;
    private final GmdBronzeWriter bronzeWriter;
    private final GmdSilverTransformer silverTransformer;

    public GmdIngestService(GmdConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new GmdClient(cfg);
        this.bronzeWriter = new GmdBronzeWriter(bronzeRoot);
        this.silverTransformer = new GmdSilverTransformer();
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
        silverTransformer.transform(bronzeRoot, silverRoot, "gmd.csv");
    }

    private String buildUrl(String key) {
        // Use GMD native API for macroeconomic data
        // GitHub raw content: https://raw.githubusercontent.com/GlobalMacroDatabase/GMD/main/Datasets/
        return "https://raw.githubusercontent.com/GlobalMacroDatabase/GMD/main/Datasets/AFG.csv";
    }
}
