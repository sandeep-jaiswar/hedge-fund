package com.hedgefund.yahoo.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedgefund.ingest.client.AbstractHttpClient;
import com.hedgefund.yahoo.config.YahooConfig;
import com.hedgefund.yahoo.model.Bar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class YahooClient extends AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(YahooClient.class);

    private final YahooConfig yahooConfig;
    private final ObjectMapper om;

    public YahooClient(YahooConfig config) {
        super(config.ingestConfig());
        this.yahooConfig = config;
        this.om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<Bar> fetchChart(String symbol) throws Exception {
        String url = yahooConfig.baseUrl() + "/v8/finance/chart/" +
            URLEncoder.encode(symbol, StandardCharsets.UTF_8) +
            "?interval=" + yahooConfig.interval() +
            "&range=" + yahooConfig.range() +
            "&includePrePost=false";

        String body = fetchWithRetry(url);
        JsonNode root = om.readTree(body);
        JsonNode result = root.path("chart").path("result");

        if (!result.isArray() || result.size() == 0) {
            throw new IOException("No result for " + symbol + ": " +
                body.substring(0, Math.min(400, body.length())));
        }

        JsonNode r = result.get(0);
        JsonNode timestamps = r.path("timestamp");
        JsonNode quote = r.path("indicators").path("quote").get(0);
        JsonNode adjclose = r.path("indicators").path("adjclose");
        JsonNode adj = null;
        if (adjclose.isArray() && adjclose.size() > 0) {
            adj = adjclose.get(0).path("adjclose");
        }

        List<Bar> bars = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

        for (int i = 0; i < timestamps.size(); i++) {
            long epoch = timestamps.get(i).asLong();
            String date = Instant.ofEpochSecond(epoch)
                .atZone(ZoneId.of("America/New_York"))
                .toLocalDate()
                .format(fmt);

            double open = quote.path("open").get(i).isNull() ? Double.NaN : quote.path("open").get(i).asDouble();
            double high = quote.path("high").get(i).isNull() ? Double.NaN : quote.path("high").get(i).asDouble();
            double low = quote.path("low").get(i).isNull() ? Double.NaN : quote.path("low").get(i).asDouble();
            double close = quote.path("close").get(i).isNull() ? Double.NaN : quote.path("close").get(i).asDouble();
            double adjc = (adj != null && adj.get(i) != null && !adj.get(i).isNull()) ? adj.get(i).asDouble() : close;
            long vol = quote.path("volume").get(i).isNull() ? 0 : quote.path("volume").get(i).asLong();

            if (Double.isNaN(open) && Double.isNaN(close)) continue;
            bars.add(new Bar(symbol, date, epoch, open, high, low, close, adjc, vol));
        }

        log.info("Fetched {} bars for {}", bars.size(), symbol);
        return bars;
    }
}
