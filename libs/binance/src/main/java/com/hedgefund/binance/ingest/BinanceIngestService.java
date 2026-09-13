package com.hedgefund.binance.ingest;

import com.hedgefund.binance.client.BinanceClient;
import com.hedgefund.binance.config.BinanceConfig;
import com.hedgefund.binance.store.BinanceBronzeWriter;
import com.hedgefund.binance.store.BinanceSilverTransformer;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class BinanceIngestService extends AbstractIngestService {

    private final BinanceConfig cfg;
    private final BinanceClient client;
    private final BinanceBronzeWriter bronzeWriter;
    private final BinanceSilverTransformer silverTransformer;

    public BinanceIngestService(BinanceConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new BinanceClient(cfg);
        this.bronzeWriter = new BinanceBronzeWriter(bronzeRoot);
        this.silverTransformer = new BinanceSilverTransformer();
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
        silverTransformer.transform(bronzeRoot, silverRoot, "binance.csv");
    }

    private String buildUrl(String key) {
        return cfg.baseUrl() + "/api/v3/klines?symbol=" + key + "&interval=" + cfg.interval() + "&limit=" + cfg.limit();
    }
}
