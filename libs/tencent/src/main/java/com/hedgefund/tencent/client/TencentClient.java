package com.hedgefund.tencent.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.tencent.config.TencentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TencentClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(TencentClient.class);

    private final TencentConfig tencentConfig;
    private final ObjectMapper om;

    public TencentClient(TencentConfig config) {
        super(config.ingestConfig());
        this.tencentConfig = config;
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
