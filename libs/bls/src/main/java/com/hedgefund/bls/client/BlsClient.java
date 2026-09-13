package com.hedgefund.bls.client;

import com.hedgefund.bls.config.BlsConfig;
import com.hedgefund.ingest.client.AbstractHttpClient;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class BlsClient extends AbstractHttpClient {

    private final BlsConfig cfg;

    public BlsClient(BlsConfig cfg) {
        super(cfg.ingestConfig());
        this.cfg = cfg;
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
}
