package com.hedgefund.defillama.client;
import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.defillama.config.DefillamaConfig;
import com.fasterxml.jackson.databind.*;
public class DefillamaClient extends AbstractHttpClient {
    private final DefillamaConfig xxxConfig;
    private final ObjectMapper om;
    public DefillamaClient(DefillamaConfig config) {
        super(config.ingestConfig());
        this.xxxConfig = config;
        this.om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    public String fetchRaw(String url) throws Exception { return fetchWithRetry(url); }
}
