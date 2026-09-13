package com.hedgefund.baostock.ingest;

import com.hedgefund.baostock.client.BaostockClient;
import com.hedgefund.baostock.config.BaostockConfig;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class BaostockIngestService extends AbstractIngestService {

    private final BaostockConfig cfg;
    private final BaostockClient client;

    public BaostockIngestService(BaostockConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new BaostockClient(cfg);
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
        silver.transformGeneric(bronzeRoot, silverRoot, "baostock.csv");
    }

    private String buildUrl(String key) {
        // Baostock requires Python SDK login flow - use Tencent proxy instead
        // Format: qt.gtimg.cn/q={symbol} (same as EastMoney/Tencent)
        return "https://qt.gtimg.cn/q=" + key;
    }
}
