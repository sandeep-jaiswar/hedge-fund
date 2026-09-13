package com.hedgefund.tencent.ingest;

import com.hedgefund.tencent.client.TencentClient;
import com.hedgefund.tencent.config.TencentConfig;
import com.hedgefund.tencent.store.TencentBronzeWriter;
import com.hedgefund.tencent.store.TencentSilverTransformer;
import java.nio.file.*;
import java.util.*;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class TencentIngestService extends AbstractIngestService {

    private final TencentConfig cfg;
    private final TencentClient client;
    private final TencentBronzeWriter bronzeWriter;
    private final TencentSilverTransformer silverTransformer;

    public TencentIngestService(TencentConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new TencentClient(cfg);
        this.bronzeWriter = new TencentBronzeWriter(bronzeRoot);
        this.silverTransformer = new TencentSilverTransformer();
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
        silverTransformer.transform(bronzeRoot, silverRoot, "tencent.csv");
    }

    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return "https://qt.gtimg.cn/q=sh600000";
    }
}
