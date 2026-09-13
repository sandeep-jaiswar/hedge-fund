package com.hedgefund.bea.client;

import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.bea.config.BeaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeaClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(BeaClient.class);

    private final BeaConfig beaConfig;

    public BeaClient(BeaConfig config) {
        super(config.ingestConfig());
        this.beaConfig = config;
    }
}
