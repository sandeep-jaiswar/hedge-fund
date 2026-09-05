package com.hedgefund.worldbank;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.hedgefund.worldbank.client.WorldBankClient;
import com.hedgefund.worldbank.config.WorldBankConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class WorldBankClientTest {
    WireMockServer wm;

    @BeforeEach
    void start(){
        wm = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wm.start();
    }
    @AfterEach
    void stop(){ wm.stop(); }

    @Test
    void fetchSinglePageCountriesAll() throws Exception {
        String json = """
                [{"page":1,"pages":1,"per_page":3,"total":2,"sourceid":"2","lastupdated":"2026-07-13"},
                 [{"indicator":{"id":"NY.GDP.MKTP.CD","value":"GDP (current US$)"},"country":{"id":"US","value":"United States"},"countryiso3code":"USA","date":"2022","value":25462700000000,"unit":"","obs_status":"","decimal":0},
                  {"indicator":{"id":"NY.GDP.MKTP.CD","value":"GDP (current US$)"},"country":{"id":"IN","value":"India"},"countryiso3code":"IND","date":"2022","value":3249938492013.47,"unit":"","obs_status":"","decimal":0}]]
                """;
        wm.stubFor(get(urlPathEqualTo("/v2/country/all/indicator/NY.GDP.MKTP.CD"))
                .withQueryParam("format", equalTo("json"))
                .willReturn(aResponse().withHeader("Content-Type","application/json").withBody(json)));

        WorldBankConfig cfg = new WorldBankConfig(wm.baseUrl()+"/v2", List.of("NY.GDP.MKTP.CD"), List.of("all"), "2022", null, null, 3, 2, 20,20,
                new WorldBankConfig.Retry(2,10,100), new WorldBankConfig.RateLimit(100,10),
                new WorldBankConfig.Paths("data/bronze/worldbank","data/silver/worldbank","catalog/glue.json"), false, 0, "2");
        var client = new WorldBankClient(cfg);
        var res = client.fetchPage(List.of("NY.GDP.MKTP.CD"), List.of("all"), "2022",1);
        assertEquals(1, res.envelope().page());
        assertEquals(2, res.points().size());
        assertEquals("USA", res.points().get(0).countryiso3code());
        assertEquals(25462700000000.0, res.points().get(0).value());
    }

    @Test
    void listAllIndicatorsPagination() throws Exception {
        String p1 = """
                [{"page":1,"pages":2,"per_page":2,"total":3},
                 [{"id":"NY.GDP.MKTP.CD","name":"GDP"},{"id":"SP.POP.TOTL","name":"Pop"}]]
                """;
        String p2 = """
                [{"page":2,"pages":2,"per_page":2,"total":3},
                 [{"id":"FP.CPI.TOTL.ZG","name":"CPI"}]]
                """;
        wm.stubFor(get(urlPathEqualTo("/v2/indicator")).withQueryParam("page", equalTo("1")).willReturn(aResponse().withBody(p1)));
        wm.stubFor(get(urlPathEqualTo("/v2/indicator")).withQueryParam("page", equalTo("2")).willReturn(aResponse().withBody(p2)));

        WorldBankConfig cfg = new WorldBankConfig(wm.baseUrl()+"/v2", List.of(), List.of("all"), "2020:2022", null,null,1000,2,20,20,
                new WorldBankConfig.Retry(2,10,100), new WorldBankConfig.RateLimit(100,10),
                new WorldBankConfig.Paths("data/bronze/worldbank","data/silver/worldbank","catalog/glue.json"), true,0, "2");
        var client = new WorldBankClient(cfg);
        var codes = client.listAllIndicatorCodes();
        assertEquals(3, codes.size());
        assertTrue(codes.contains("FP.CPI.TOTL.ZG"));
    }
}
