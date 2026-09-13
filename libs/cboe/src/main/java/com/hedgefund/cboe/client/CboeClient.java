package com.hedgefund.cboe.client;

import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.cboe.config.CboeConfig;

public class CboeClient extends AbstractHttpClient {

    public CboeClient(CboeConfig config) {
        super(config.ingestConfig());
    }
}
