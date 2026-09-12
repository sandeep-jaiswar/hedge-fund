package com.hedgefund.coinbase.client;
import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.coinbase.config.CoinbaseConfig;
import com.fasterxml.jackson.databind.*;
public class CoinbaseClient extends AbstractHttpClient {
    private final CoinbaseConfig xxxConfig;
    private final ObjectMapper om;
    public CoinbaseClient(CoinbaseConfig config) {
        super(config.ingestConfig());
        this.xxxConfig = config;
        this.om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    public String fetchRaw(String url) throws Exception { return fetchWithRetry(url); }
}
