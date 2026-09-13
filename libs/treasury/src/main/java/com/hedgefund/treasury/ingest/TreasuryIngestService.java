package com.hedgefund.treasury.ingest;

import com.hedgefund.treasury.client.TreasuryClient;
import com.hedgefund.treasury.config.TreasuryConfig;
import com.hedgefund.treasury.store.TreasuryBronzeWriter;
import com.hedgefund.treasury.store.TreasurySilverTransformer;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class TreasuryIngestService extends AbstractIngestService {

    private final TreasuryConfig cfg;
    private final TreasuryClient client;
    private final TreasuryBronzeWriter bronzeWriter;
    private final TreasurySilverTransformer silverTransformer;

    public TreasuryIngestService(TreasuryConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new TreasuryClient(cfg);
        this.bronzeWriter = new TreasuryBronzeWriter(bronzeRoot);
        this.silverTransformer = new TreasurySilverTransformer();
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
        silverTransformer.transform(bronzeRoot, silverRoot, "treasury.csv");
    }

    private String buildUrl(String key) {
        int year = Integer.parseInt(key.replace("treasury_", "").replace("treasury-", "").replaceAll("[^0-9]", ""));
        if (year < 2016 || year > 2026) year = 2024;
        return cfg.baseUrl() + "/resource-center/data-chart-center/interest-rates/daily-treasury-rates.csv/" + year + "/all?type=daily_treasury_yield_curve";
    }
}
