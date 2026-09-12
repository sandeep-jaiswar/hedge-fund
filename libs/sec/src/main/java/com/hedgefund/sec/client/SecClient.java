package com.hedgefund.sec.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.sec.config.SecConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SecClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(SecClient.class);

    private final SecConfig secConfig;
    private final ObjectMapper om;

    public SecClient(SecConfig config) {
        super(config.ingestConfig());
        this.secConfig = config;
        this.om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    protected String fetchWithRetry(String url) throws Exception {
        int attempts = 0;
        long backoff = config.retry().backoffMs();

        while (true) {
            attempts++;
            try {
                throttle();
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .GET()
                    .header("User-Agent", "HedgeFund 1.0 (jaiswarsandeep119@gmail.com)")
                    .header("Accept", "application/json,*/*")
                    .header("Accept-Encoding", "identity")
                    .build();
                HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 429 || resp.statusCode() >= 500) {
                    throw new java.io.IOException("HTTP " + resp.statusCode() + " " +
                        resp.body().substring(0, Math.min(300, resp.body().length())));
                }
                if (resp.statusCode() != 200) {
                    throw new java.io.IOException("HTTP " + resp.statusCode() + " " +
                        resp.body().substring(0, Math.min(400, resp.body().length())));
                }
                return resp.body();
            } catch (Exception e) {
                if (attempts >= config.retry().maxAttempts()) throw e;
                log.warn("Fetch failed {}/{} {} -> retry {}ms {}",
                    attempts, config.retry().maxAttempts(), url, backoff, e.toString());
                Thread.sleep(backoff + java.util.concurrent.ThreadLocalRandom.current().nextInt(300));
                backoff = Math.min(backoff * 2, config.retry().maxBackoffMs());
            }
        }
    }

    public String fetchRaw(String url) throws Exception {
        return fetchWithRetry(url);
    }

    public JsonNode fetchJson(String url) throws Exception {
        String raw = fetchWithRetry(url);
        return om.readTree(raw);
    }
}
