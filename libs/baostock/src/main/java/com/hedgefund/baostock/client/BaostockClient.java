package com.hedgefund.baostock.client;

import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.baostock.config.BaostockConfig;

public class BaostockClient extends AbstractHttpClient {

    public BaostockClient(BaostockConfig config) {
        super(config.ingestConfig());
    }
}
