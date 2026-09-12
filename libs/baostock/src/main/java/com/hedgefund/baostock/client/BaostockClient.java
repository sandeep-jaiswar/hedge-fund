package com.hedgefund.baostock.client;
import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.baostock.config.BaostockConfig;
import com.fasterxml.jackson.databind.*;
public class BaostockClient extends AbstractHttpClient {
    private final BaostockConfig xxxConfig;
    private final ObjectMapper om;
    public BaostockClient(BaostockConfig config) {
        super(config.ingestConfig());
        this.xxxConfig = config;
        this.om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    public String fetchRaw(String url) throws Exception { return fetchWithRetry(url); }
}
