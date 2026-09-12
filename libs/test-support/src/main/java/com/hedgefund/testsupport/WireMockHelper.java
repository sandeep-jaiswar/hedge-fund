package com.hedgefund.testsupport;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WireMockHelper {

    private final WireMockServer server;

    public WireMockHelper() {
        this(WireMockConfiguration.wireMockConfig().dynamicPort());
    }

    public WireMockHelper(WireMockConfiguration config) {
        this.server = new WireMockServer(config);
    }

    public void start() {
        server.start();
        WireMock.configureFor("localhost", server.port());
    }

    public void stop() {
        if (server.isRunning()) {
            server.stop();
        }
    }

    public int port() {
        return server.port();
    }

    public String baseUrl() {
        return "http://localhost:" + server.port();
    }

    public void stubGet(String url, int status, String body) {
        stubFor(get(urlEqualTo(url))
            .willReturn(aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
    }

    public void stubGetWithDelay(String url, int status, String body, int delayMs) {
        stubFor(get(urlEqualTo(url))
            .willReturn(aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body)
                .withFixedDelay(delayMs)));
    }

    public void stubGetJson(String url, String jsonBody) {
        stubGet(url, 200, jsonBody);
    }

    public void verifyGetCalled(String url, int count) {
        verify(count, getRequestedFor(urlEqualTo(url)));
    }
}
