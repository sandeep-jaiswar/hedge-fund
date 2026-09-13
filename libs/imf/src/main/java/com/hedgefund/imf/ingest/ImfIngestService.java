package com.hedgefund.imf.ingest;

import com.hedgefund.imf.client.ImfClient;
import com.hedgefund.imf.config.ImfConfig;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class ImfIngestService extends AbstractIngestService {

    private final ImfConfig cfg;
    private final ImfClient client;

    public ImfIngestService(ImfConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new ImfClient(cfg);
    }

    @Override
    protected List<String> keys() {
        return cfg.effectiveKeys();
    }

    @Override
    protected void ingestSymbol(String symbol) throws Exception {
        bronze.writeRawJson(symbol, client.fetchRaw(buildUrl(symbol)));
    }

    @Override
    protected void transformSilver() throws Exception {
        silver.transformGeneric(bronzeRoot, silverRoot, "imf.csv");
    }

    private String buildUrl(String key) {
        // Use IMF SDMX JSON API (HTTP, not HTTPS) for reliable access
        // Format: http://dataservices.imf.org/REST/SDMX_JSON.svc/CompactData/IFS/{period}/{country}.{indicator}
        // Example: GDP growth for US 2023
        return "http://dataservices.imf.org/REST/SDMX_JSON.svc/CompactData/IFS/2023/US.NGDP_RPCH";
    }
}
