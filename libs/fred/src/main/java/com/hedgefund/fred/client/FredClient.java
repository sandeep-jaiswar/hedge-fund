package com.hedgefund.fred.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.fred.config.FredConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FredClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(FredClient.class);

    private final FredConfig fredConfig;
    private final ObjectMapper om;

    public FredClient(FredConfig config) {
        super(config.ingestConfig());
        this.fredConfig = config;
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
