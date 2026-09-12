package com.hedgefund.tencent.client;
import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.tencent.config.TencentConfig;
import com.fasterxml.jackson.databind.*;
public class TencentClient extends AbstractHttpClient {
    private final TencentConfig xxxConfig;
    private final ObjectMapper om;
    public TencentClient(TencentConfig config) {
        super(config.ingestConfig());
        this.xxxConfig = config;
        this.om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    public String fetchRaw(String url) throws Exception { return fetchWithRetry(url); }
}
