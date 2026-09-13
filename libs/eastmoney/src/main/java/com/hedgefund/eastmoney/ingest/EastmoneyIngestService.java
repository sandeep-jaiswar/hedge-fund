package com.hedgefund.eastmoney.ingest;

import com.hedgefund.eastmoney.client.EastmoneyClient;
import com.hedgefund.eastmoney.config.EastmoneyConfig;
import com.hedgefund.eastmoney.store.EastmoneyBronzeWriter;
import com.hedgefund.eastmoney.store.EastmoneySilverTransformer;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class EastmoneyIngestService extends AbstractIngestService {

    private final EastmoneyConfig cfg;
    private final EastmoneyClient client;
    private final EastmoneyBronzeWriter bronzeWriter;
    private final EastmoneySilverTransformer silverTransformer;

    public EastmoneyIngestService(EastmoneyConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new EastmoneyClient(cfg);
        this.bronzeWriter = new EastmoneyBronzeWriter(bronzeRoot);
        this.silverTransformer = new EastmoneySilverTransformer();
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
        silverTransformer.transform(bronzeRoot, silverRoot, "eastmoney.csv");
    }

    private String buildUrl(String key) {
        String base = cfg.baseUrl();
        return "https://qt.gtimg.cn/q=sh600000";
    }
}
