package com.hedgefund.eia.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedgefund.eia.config.EiaConfig;
import com.hedgefund.ingest.client.AbstractHttpClient;

public class EiaClient extends AbstractHttpClient {
    private final EiaConfig cfg;
    private final ObjectMapper om;

    public EiaClient(EiaConfig cfg) {
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
