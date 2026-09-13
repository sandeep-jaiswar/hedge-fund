package com.hedgefund.fred.client;

import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.fred.config.FredConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FredClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(FredClient.class);

    private final FredConfig fredConfig;

    public FredClient(FredConfig config) {
        super(config.ingestConfig());
        this.fredConfig = config;
    }
}
