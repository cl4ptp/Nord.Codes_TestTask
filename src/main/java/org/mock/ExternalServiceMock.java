package org.mock;


import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class ExternalServiceMock {
    private final WireMockServer wireMockServer;

    public ExternalServiceMock(int port) {
        this.wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port));
    }

    public void start() {
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    public void stop() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    public void reset() {
        wireMockServer.resetAll();
    }
}
