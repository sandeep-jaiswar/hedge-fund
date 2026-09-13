package com.hedgefund.treasury.client;

import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.treasury.config.TreasuryConfig;

public class TreasuryClient extends AbstractHttpClient {

    private final TreasuryConfig treasuryConfig;

    public TreasuryClient(TreasuryConfig config) {
        super(config.ingestConfig());
        this.treasuryConfig = config;
    }
}
