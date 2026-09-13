package com.hedgefund.defillama.client;

import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.defillama.config.DefillamaConfig;

public class DefillamaClient extends AbstractHttpClient {

    public DefillamaClient(DefillamaConfig config) {
        super(config.ingestConfig());
    }
}
