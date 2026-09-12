package com.hedgefund.fdic.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedgefund.fdic.config.FdicConfig;
import com.hedgefund.ingest.client.AbstractHttpClient;

public class FdicClient extends AbstractHttpClient {
    private final FdicConfig cfg;
    private final ObjectMapper om;

    public FdicClient(FdicConfig cfg) {
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
