package com.hedgefund.bls.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedgefund.bls.config.BlsConfig;
import com.hedgefund.ingest.client.AbstractHttpClient;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class BlsClient extends AbstractHttpClient {
    private final BlsConfig cfg;
    private final ObjectMapper om;

    public BlsClient(BlsConfig cfg) {
        super(cfg.ingestConfig());
        this.cfg = cfg;
        this.om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String fetchRaw(String url) throws Exception {
        return fetchWithRetry(url);
    }

    /**
     * Fetch BLS data using the public API v2 with POST request.
     * Free tier: 25 requests/day without API key.
     */
    public String fetchBlsSeries(List<String> seriesIds) throws Exception {
        String apiKey = cfg.apiKey();
        String apiUrl = "https://api.bls.gov/publicAPI/v2/timeseries/data/";
        String seriesList = String.join("\",\"", seriesIds);
        String requestBody = String.format(
            "{\"seriesid\": [\"%s\"], \"startyear\": \"2020\", \"endyear\": \"2025\"}",
            seriesList
        );
        String url = (apiKey != null && !apiKey.isEmpty())
            ? apiUrl + "?registrationkey=" + apiKey
            : apiUrl;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public JsonNode fetchJson(String url) throws Exception {
        String raw = fetchRaw(url);
        return om.readTree(raw);
    }
}
