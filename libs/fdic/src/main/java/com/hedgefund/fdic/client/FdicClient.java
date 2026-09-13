package com.hedgefund.fdic.client;

import com.hedgefund.fdic.config.FdicConfig;
import com.hedgefund.ingest.client.AbstractHttpClient;

public class FdicClient extends AbstractHttpClient {

    private final FdicConfig cfg;

    public FdicClient(FdicConfig cfg) {
        super(cfg.ingestConfig());
        this.cfg = cfg;
    }
}
