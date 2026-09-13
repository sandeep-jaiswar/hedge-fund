package com.hedgefund.sec.client;

import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.sec.config.SecConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(SecClient.class);

    private final SecConfig secConfig;

    public SecClient(SecConfig config) {
        super(config.ingestConfig());
        this.secConfig = config;
    }
}
