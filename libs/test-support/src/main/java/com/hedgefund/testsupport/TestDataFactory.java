package com.hedgefund.testsupport;

import com.hedgefund.ingest.config.IngestConfig;

import java.util.List;

public final class TestDataFactory {

    private TestDataFactory() {}

    public static IngestConfig sampleIngestConfig(String sourceId) {
        return new IngestConfig(
            sourceId,
            "https://httpbin.org",
            List.of("TEST1", "TEST2"),
            2,
            new IngestConfig.Retry(2, 100, 1000),
            new IngestConfig.RateLimit(1, 2),
            new IngestConfig.Paths(
                "data/bronze/" + sourceId,
                "data/silver/" + sourceId,
                "catalog/glue.json"
            )
        );
    }

    public static String sampleNdjsonBar(String symbol, String date) {
        return "{\"symbol\":\"" + symbol + "\",\"date\":\"" + date +
            "\",\"epoch\":1700000000,\"open\":100.0,\"high\":105.0," +
            "\"low\":99.0,\"close\":103.0,\"adjClose\":103.0,\"volume\":1000000}";
    }

    public static String sampleNdjsonBar(String symbol, String date, double close) {
        return "{\"symbol\":\"" + symbol + "\",\"date\":\"" + date +
            "\",\"epoch\":1700000000,\"open\":100.0,\"high\":105.0," +
            "\"low\":99.0,\"close\":" + close + ",\"adjClose\":" + close + ",\"volume\":1000000}";
    }

    public static String sampleYahooChartResponse(String symbol) {
        return """
            {"chart":{"result":[{"meta":{"symbol":"%s"},"timestamp":[1700000000,1700086400],"indicators":{"quote":[{"open":[100.0,101.0],"high":[105.0,106.0],"low":[99.0,100.0],"close":[103.0,104.0],"volume":[1000000,1100000]}],"adjclose":[{"adjclose":[103.0,104.0]}]}}]}}""".formatted(symbol);
    }

    public static String sampleBinanceKlinesResponse(String symbol) {
        return "[[1700000000000,\"100.00\",\"105.00\",\"99.00\",\"103.00\",\"1000000\",1700086400000,\"1000000\",1000,\"500000\",\"1000000\",0]]";
    }

    public static String sampleFredObservationsResponse(String seriesId) {
        return """
            {"realtime_start":"2024-01-01","realtime_end":"2024-01-01","observation_start":"1970-01-01","observation_end":"2024-01-01","units":"lin","output_type":1,"file_type":json","order_by":"observation_date","sort_order":"asc","count":1,"offset":0,"limit":1,"observations":[{"realtime_start":"2024-01-01","realtime_end":"2024-01-01","date":"2024-01-01","value":"4.25"}]}""";
    }
}
