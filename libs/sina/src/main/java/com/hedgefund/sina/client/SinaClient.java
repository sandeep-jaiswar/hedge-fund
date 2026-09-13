package com.hedgefund.sina.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.sina.config.SinaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SinaClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(SinaClient.class);

    private final SinaConfig sinaConfig;
    private final ObjectMapper om;

    public SinaClient(SinaConfig config) {
        super(config.ingestConfig());
        this.sinaConfig = config;
        this.om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    protected Map<String, String> defaultHeaders() {
        return Map.of("Referer", "https://finance.sina.com.cn");
    }

    public String fetchRaw(String url) throws Exception {
        return fetchWithRetry(url);
    }

    public JsonNode fetchJson(String url) throws Exception {
        String raw = fetchWithRetry(url);
        return om.readTree(raw);
    }
}
