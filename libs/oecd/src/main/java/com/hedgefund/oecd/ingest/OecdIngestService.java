package com.hedgefund.oecd.ingest;

import com.hedgefund.oecd.client.OecdClient;
import com.hedgefund.oecd.config.OecdConfig;
import com.hedgefund.oecd.store.OecdBronzeWriter;
import com.hedgefund.oecd.store.OecdSilverTransformer;
import java.nio.file.Path;
import java.util.List;
import com.hedgefund.ingest.service.AbstractIngestService;

/** Thin source adapter: URL + fetch only. Concurrency, retry, watermark via framework. */
public class OecdIngestService extends AbstractIngestService {

    private final OecdConfig cfg;
    private final OecdClient client;
    private final OecdBronzeWriter bronzeWriter;
    private final OecdSilverTransformer silverTransformer;

    public OecdIngestService(OecdConfig cfg, Path datalakeRoot) {
        super(cfg.ingestConfig(), datalakeRoot);
        this.cfg = cfg;
        this.client = new OecdClient(cfg);
        this.bronzeWriter = new OecdBronzeWriter(bronzeRoot);
        this.silverTransformer = new OecdSilverTransformer();
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
        silverTransformer.transform(bronzeRoot, silverRoot, "oecd.csv");
    }

    private String buildUrl(String key) {
        // OECD SDMX API requires 7-dimension key format
        // Format: /public/rest/data/{dataset},{observation}.{version}/{country}.{measure}.{frequency}.{adjustment}.{base-period}.{series}.{indicator}
        // Example: Consumer price inflation for USA
        return "https://sdmx.oecd.org/public/rest/data/OECD.SDD.STES,DSD_KEI@DF_KEI,4.0/USA.CP_TOTL_ZG.M.NSA.SP.A.Q";
    }
}
