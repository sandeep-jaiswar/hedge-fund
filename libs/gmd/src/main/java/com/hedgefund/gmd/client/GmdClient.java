package com.hedgefund.gmd.client;

import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.gmd.config.GmdConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GmdClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(GmdClient.class);

    private final GmdConfig gmdConfig;

    public GmdClient(GmdConfig config) {
        super(config.ingestConfig());
        this.gmdConfig = config;
    }
}
