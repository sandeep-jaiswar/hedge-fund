package com.hedgefund.bls.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedgefund.bls.config.BlsConfig;
import com.hedgefund.ingest.client.AbstractHttpClient;

public class BlsClient extends AbstractHttpClient {
    private final BlsConfig cfg;
    private final ObjectMapper om;

    public BlsClient(BlsConfig cfg) {
        super(cfg.ingestConfig());
        this.cfg = cfg;
        this.om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String fetchRaw(String url) throws Exception {
        return fetchWithRetry(url);
    }

    public JsonNode fetchJson(String url) throws Exception {
        String raw = fetchRaw(url);
        return om.readTree(raw);
    }
}
