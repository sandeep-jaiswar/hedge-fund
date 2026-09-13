package com.hedgefund.bea.ingest;

import com.hedgefund.bea.client.BeaClient;
import com.hedgefund.bea.config.BeaConfig;
import com.hedgefund.bea.store.BeaBronzeWriter;
import com.hedgefund.bea.store.BeaSilverTransformer;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class BeaIngestService extends AbstractIngestService {

    private final BeaConfig cfg;
    private final BeaClient client;
    private final BeaBronzeWriter bronzeWriter;
    private final BeaSilverTransformer silverTransformer;

    public BeaIngestService(BeaConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new BeaClient(cfg);
        this.bronzeWriter = new BeaBronzeWriter(bronzeRoot);
        this.silverTransformer = new BeaSilverTransformer();
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
        silverTransformer.transform(bronzeRoot, silverRoot, "bea.csv");
    }

    private String buildUrl(String key) {
        String base = cfg.baseUrl();
        return "https://apps.bea.gov/api/data?UserID=demo&method=GetData&DataSetName=NIPA&TableName=T10101&Frequency=Q&Year=2016,2017,2018,2019,2020,2021,2022,2023,2024,2025,2026";
    }
}
