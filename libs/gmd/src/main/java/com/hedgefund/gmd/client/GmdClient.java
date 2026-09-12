package com.hedgefund.gmd.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.gmd.config.GmdConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GmdClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(GmdClient.class);

    private final GmdConfig gmdConfig;
    private final ObjectMapper om;

    public GmdClient(GmdConfig config) {
        super(config.ingestConfig());
        this.gmdConfig = config;
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
