package com.hedgefund.calcfi.client;

import com.hedgefund.calcfi.config.CalcfiConfig;
import com.hedgefund.ingest.client.AbstractHttpClient;

public class CalcfiClient extends AbstractHttpClient {

    private final CalcfiConfig cfg;

    public CalcfiClient(CalcfiConfig cfg) {
        super(cfg.ingestConfig());
        this.cfg = cfg;
    }
}
