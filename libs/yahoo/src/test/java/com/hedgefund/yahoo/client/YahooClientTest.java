package com.hedgefund.yahoo.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.hedgefund.testsupport.WireMockHelper;
import com.hedgefund.testsupport.TestDataFactory;
import com.hedgefund.yahoo.config.YahooConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class YahooClientTest {

    private WireMockHelper wireMock;
    private YahooClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockHelper();
        wireMock.start();

        YahooConfig cfg = new YahooConfig(
            wireMock.baseUrl(),
            List.of("AAPL", "MSFT"),
            "1d",
            "1mo",
            com.hedgefund.ingest.config.IngestConfig.defaults("yahoo")
        );
        client = new YahooClient(cfg);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void fetchChartReturnsBars() throws Exception {
        stubFor(get(urlMatching(".*/v8/finance/chart/.*"))
            .willReturn(okJson(TestDataFactory.sampleYahooChartResponse("AAPL"))));

        var bars = client.fetchChart("AAPL");

        assertNotNull(bars);
        assertFalse(bars.isEmpty());
        assertEquals("AAPL", bars.get(0).symbol());
    }

    @Test
    void fetchChartHandlesEmptyResult() {
        stubFor(get(urlMatching(".*/v8/finance/chart/.*"))
            .willReturn(okJson("{\"chart\":{\"result\":[]}}")));

        assertThrows(Exception.class, () -> client.fetchChart("INVALID"));
    }

    @Test
    void fetchChartRetriesOn500() throws Exception {
        stubFor(get(urlMatching(".*/v8/finance/chart/.*"))
            .willReturn(serverError())
            .willReturn(okJson(TestDataFactory.sampleYahooChartResponse("AAPL"))));

        var bars = client.fetchChart("AAPL");
        assertNotNull(bars);
        assertFalse(bars.isEmpty());
    }

    @Test
    void fetchChartRetriesOn429() throws Exception {
        stubFor(get(urlMatching(".*/v8/finance/chart/.*"))
            .willReturn(aResponse().withStatus(429).withBody("Rate limited"))
            .willReturn(okJson(TestDataFactory.sampleYahooChartResponse("AAPL"))));

        var bars = client.fetchChart("AAPL");
        assertNotNull(bars);
        assertFalse(bars.isEmpty());
    }
}
