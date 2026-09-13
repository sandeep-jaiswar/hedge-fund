package com.hedgefund.eastmoney.client;

import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.eastmoney.config.EastmoneyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EastmoneyClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(EastmoneyClient.class);

    private final EastmoneyConfig eastmoneyConfig;

    public EastmoneyClient(EastmoneyConfig config) {
        super(config.ingestConfig());
        this.eastmoneyConfig = config;
    }
}
