package com.hedgefund.sina.client;

import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.sina.config.SinaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SinaClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(SinaClient.class);

    private final SinaConfig sinaConfig;

    public SinaClient(SinaConfig config) {
        super(config.ingestConfig());
        this.sinaConfig = config;
    }

    @Override
    protected Map<String, String> defaultHeaders() {
        return Map.of("Referer", "https://finance.sina.com.cn");
    }
}
