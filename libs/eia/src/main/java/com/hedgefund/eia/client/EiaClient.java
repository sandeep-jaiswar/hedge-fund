package com.hedgefund.eia.client;

import com.hedgefund.eia.config.EiaConfig;
import com.hedgefund.ingest.client.AbstractHttpClient;

public class EiaClient extends AbstractHttpClient {

    private final EiaConfig cfg;

    public EiaClient(EiaConfig cfg) {
        super(cfg.ingestConfig());
        this.cfg = cfg;
    }
}
