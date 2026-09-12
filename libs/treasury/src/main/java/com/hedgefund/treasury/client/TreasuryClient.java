package com.hedgefund.treasury.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.treasury.config.TreasuryConfig;

public class TreasuryClient extends AbstractHttpClient {

    private final TreasuryConfig treasuryConfig;
    private final ObjectMapper om;

    public TreasuryClient(TreasuryConfig config) {
        super(config.ingestConfig());
        this.treasuryConfig = config;
        this.om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String fetchRaw(String url) throws Exception {
        return fetchWithRetry(url);
    }

    public JsonNode fetchJson(String url) throws Exception {
        String raw = fetchWithRetry(url);
        return om.readTree(raw);
    }
}
