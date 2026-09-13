package com.hedgefund.binance.client;

import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.binance.config.BinanceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinanceClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(BinanceClient.class);

    private final BinanceConfig binanceConfig;

    public BinanceClient(BinanceConfig config) {
        super(config.ingestConfig());
        this.binanceConfig = config;
    }
}
