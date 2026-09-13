package com.hedgefund.baostock.ingest;

import com.hedgefund.baostock.client.BaostockClient;
import com.hedgefund.baostock.config.BaostockConfig;
import com.hedgefund.baostock.store.BaostockBronzeWriter;
import com.hedgefund.baostock.store.BaostockSilverTransformer;
import java.nio.file.*;
import java.util.*;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class BaostockIngestService extends AbstractIngestService {

    private final BaostockConfig cfg;
    private final BaostockClient client;
    private final BaostockBronzeWriter bronzeWriter;
    private final BaostockSilverTransformer silverTransformer;

    public BaostockIngestService(BaostockConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new BaostockClient(cfg);
        this.bronzeWriter = new BaostockBronzeWriter(bronzeRoot);
        this.silverTransformer = new BaostockSilverTransformer();
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
        silverTransformer.transform(bronzeRoot, silverRoot, "baostock.csv");
    }

    private String buildUrl(String key) {
        // Baostock requires Python SDK login flow - use Tencent proxy instead
        // Format: qt.gtimg.cn/q={symbol} (same as EastMoney/Tencent)
        return "https://qt.gtimg.cn/q=" + key;
    }
}
