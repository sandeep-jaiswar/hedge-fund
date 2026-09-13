package com.hedgefund.tencent.ingest;

import com.hedgefund.tencent.client.TencentClient;
import com.hedgefund.tencent.config.TencentConfig;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class TencentIngestService extends AbstractIngestService {

    private final TencentConfig cfg;
    private final TencentClient client;

    public TencentIngestService(TencentConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new TencentClient(cfg);
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
        silver.transformGeneric(bronzeRoot, silverRoot, "tencent.csv");
    }

    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return "https://qt.gtimg.cn/q=sh600000";
    }
}
