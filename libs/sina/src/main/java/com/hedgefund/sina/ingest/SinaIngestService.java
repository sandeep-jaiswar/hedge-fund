package com.hedgefund.sina.ingest;

import com.hedgefund.sina.client.SinaClient;
import com.hedgefund.sina.config.SinaConfig;
import com.hedgefund.sina.store.SinaBronzeWriter;
import com.hedgefund.sina.store.SinaSilverTransformer;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class SinaIngestService extends AbstractIngestService {

    private final SinaConfig cfg;
    private final SinaClient client;
    private final SinaBronzeWriter bronzeWriter;
    private final SinaSilverTransformer silverTransformer;

    public SinaIngestService(SinaConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new SinaClient(cfg);
        this.bronzeWriter = new SinaBronzeWriter(bronzeRoot);
        this.silverTransformer = new SinaSilverTransformer();
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
        silverTransformer.transform(bronzeRoot, silverRoot, "sina.csv");
    }

    private String buildUrl(String key) {
        String base = cfg.baseUrl();
        return "https://hq.sinajs.cn/list=sh600000";
    }
}
