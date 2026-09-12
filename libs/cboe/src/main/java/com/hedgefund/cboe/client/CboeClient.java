package com.hedgefund.cboe.client;
import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.cboe.config.CboeConfig;
import com.fasterxml.jackson.databind.*;
public class CboeClient extends AbstractHttpClient {
    private final CboeConfig xxxConfig;
    private final ObjectMapper om;
    public CboeClient(CboeConfig config) {
        super(config.ingestConfig());
        this.xxxConfig = config;
        this.om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    public String fetchRaw(String url) throws Exception { return fetchWithRetry(url); }
}
