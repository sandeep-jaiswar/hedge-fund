package com.hedgefund.coinbase.client;

import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.coinbase.config.CoinbaseConfig;

public class CoinbaseClient extends AbstractHttpClient {

    public CoinbaseClient(CoinbaseConfig config) {
        super(config.ingestConfig());
    }
}
