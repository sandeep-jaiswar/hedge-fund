package com.hedgefund.investing.client;

import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.investing.config.InvestingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvestingClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(InvestingClient.class);

    private final InvestingConfig investingConfig;

    public InvestingClient(InvestingConfig config) {
        super(config.ingestConfig());
        this.investingConfig = config;
    }
}
