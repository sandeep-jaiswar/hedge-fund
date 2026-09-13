package com.hedgefund.imf.client;

import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.imf.config.ImfConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImfClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(ImfClient.class);

    private final ImfConfig imfConfig;

    public ImfClient(ImfConfig config) {
        super(config.ingestConfig());
        this.imfConfig = config;
    }
}
