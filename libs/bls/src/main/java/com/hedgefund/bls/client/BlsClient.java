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
     * Requires registration for a free API key (500 requests/day).
     * Falls back to CSV download if API key is not provided.
     */
    public String fetchBlsSeries(List<String> seriesIds) throws Exception {
        String apiKey = cfg.apiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            return fetchViaApi(seriesIds, apiKey);
        } else {
            return fetchViaCsv(seriesIds);
        }
    }

    private String fetchViaApi(List<String> seriesIds, String apiKey) throws Exception {
        String apiUrl = "https://api.bls.gov/publicAPI/v2/timeseries/data/";
        String seriesList = String.join(",", seriesIds);
        String requestBody = String.format(
            "{\"seriesid\": [\"%s\"], \"startyear\": \"2020\", \"endyear\": \"2024\"}",
            seriesList.replace(",", "\",\"")
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl + "?registrationkey=" + apiKey))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private String fetchViaCsv(List<String> seriesIds) throws Exception {
        // Fallback to CSV download (blocked by WAF, but keeps existing behavior)
        return fetchWithRetry("https://download.bls.gov/pub/time.series/ap/ap.data.0.Current");
    }

    public JsonNode fetchJson(String url) throws Exception {
        String raw = fetchRaw(url);
        return om.readTree(raw);
    }
}
