package com.hedgefund.coinbase.ingest;

import com.hedgefund.coinbase.client.CoinbaseClient;
import com.hedgefund.coinbase.config.CoinbaseConfig;
import com.hedgefund.coinbase.store.CoinbaseBronzeWriter;
import com.hedgefund.coinbase.store.CoinbaseSilverTransformer;
import java.nio.file.*;
import java.util.*;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class CoinbaseIngestService extends AbstractIngestService {

    private final CoinbaseConfig cfg;
    private final CoinbaseClient client;
    private final CoinbaseBronzeWriter bronzeWriter;
    private final CoinbaseSilverTransformer silverTransformer;

    public CoinbaseIngestService(CoinbaseConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new CoinbaseClient(cfg);
        this.bronzeWriter = new CoinbaseBronzeWriter(bronzeRoot);
        this.silverTransformer = new CoinbaseSilverTransformer();
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
        silverTransformer.transform(bronzeRoot, silverRoot, "coinbase.csv");
    }

    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return base+"/v2/prices/"+key+"/spot";
    }
}
