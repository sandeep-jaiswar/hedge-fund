package com.hedgefund.oecd.client;

import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.oecd.config.OecdConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OecdClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(OecdClient.class);

    private final OecdConfig oecdConfig;

    public OecdClient(OecdConfig config) {
        super(config.ingestConfig());
        this.oecdConfig = config;
    }
}
