package com.hedgefund.tencent.client;

import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.tencent.config.TencentConfig;

public class TencentClient extends AbstractHttpClient {

    public TencentClient(TencentConfig config) {
        super(config.ingestConfig());
    }
}
