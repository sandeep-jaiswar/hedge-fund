package com.hedgefund.binance.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.hedgefund.testsupport.WireMockHelper;
import com.hedgefund.binance.config.BinanceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class BinanceClientTest {

    private WireMockHelper wireMock;
    private BinanceClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockHelper();
        wireMock.start();

        BinanceConfig cfg = new BinanceConfig(
            wireMock.baseUrl(),
            List.of("BTCUSDT", "ETHUSDT"),
            "1d",
            30,
            com.hedgefund.ingest.config.IngestConfig.defaults("binance")
        );
        client = new BinanceClient(cfg);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void fetchKlinesReturnsData() throws Exception {
        stubFor(get(urlMatching(".*/api/v3/klines.*"))
            .willReturn(okJson("[[1700000000000,\"100.00\",\"105.00\",\"99.00\",\"103.00\",\"1000000\",1700086400000,\"1000000\",1000,\"500000\",\"1000000\",0]]")));

        var response = client.fetchRaw(wireMock.baseUrl() + "/api/v3/klines?symbol=BTCUSDT&interval=1d&limit=1");

        assertNotNull(response);
        assertTrue(response.contains("100.00"));
    }

    @Test
    void fetchKlinesHandlesEmptyResult() throws Exception {
        stubFor(get(urlMatching(".*/api/v3/klines.*"))
            .willReturn(okJson("[]")));

        var response = client.fetchRaw(wireMock.baseUrl() + "/api/v3/klines?symbol=INVALID&interval=1d&limit=1");

        assertNotNull(response);
        assertEquals("[]", response);
    }

    @Test
    void fetchKlinesRetriesOn500() throws Exception {
        stubFor(get(urlMatching(".*/api/v3/klines.*"))
            .willReturn(serverError())
            .willReturn(okJson("[[1700000000000,\"100.00\",\"105.00\",\"99.00\",\"103.00\",\"1000000\",1700086400000,\"1000000\",1000,\"500000\",\"1000000\",0]]")));

        var response = client.fetchRaw(wireMock.baseUrl() + "/api/v3/klines?symbol=BTCUSDT&interval=1d&limit=1");
        assertNotNull(response);
        assertTrue(response.contains("100.00"));
    }

    @Test
    void fetchKlinesRetriesOn429() throws Exception {
        stubFor(get(urlMatching(".*/api/v3/klines.*"))
            .willReturn(aResponse().withStatus(429).withBody("Rate limited"))
            .willReturn(okJson("[[1700000000000,\"100.00\",\"105.00\",\"99.00\",\"103.00\",\"1000000\",1700086400000,\"1000000\",1000,\"500000\",\"1000000\",0]]")));

        var response = client.fetchRaw(wireMock.baseUrl() + "/api/v3/klines?symbol=BTCUSDT&interval=1d&limit=1");
        assertNotNull(response);
        assertTrue(response.contains("100.00"));
    }
}
