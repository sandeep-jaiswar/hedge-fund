package com.hedgefund.imf.ingest;

import com.hedgefund.imf.client.ImfClient;
import com.hedgefund.imf.config.ImfConfig;
import com.hedgefund.imf.store.ImfBronzeWriter;
import com.hedgefund.imf.store.ImfSilverTransformer;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class ImfIngestService extends AbstractIngestService {

    private final ImfConfig cfg;
    private final ImfClient client;
    private final ImfBronzeWriter bronzeWriter;
    private final ImfSilverTransformer silverTransformer;

    public ImfIngestService(ImfConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new ImfClient(cfg);
        this.bronzeWriter = new ImfBronzeWriter(bronzeRoot);
        this.silverTransformer = new ImfSilverTransformer();
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
        silverTransformer.transform(bronzeRoot, silverRoot, "imf.csv");
    }

    private String buildUrl(String key) {
        // Use IMF SDMX JSON API (HTTP, not HTTPS) for reliable access
        // Format: http://dataservices.imf.org/REST/SDMX_JSON.svc/CompactData/IFS/{period}/{country}.{indicator}
        // Example: GDP growth for US 2023
        return "http://dataservices.imf.org/REST/SDMX_JSON.svc/CompactData/IFS/2023/US.NGDP_RPCH";
    }
}
