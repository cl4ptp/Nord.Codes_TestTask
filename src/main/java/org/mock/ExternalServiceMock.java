package org.mock;


import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.qameta.allure.Step;

import static com.github.tomakehurst.wiremock.client.WireMock.*;


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

    @Step("Mock external service: successful authentication (/auth -> 200 OK) for token '{token}'")
    public void stubAuthSuccess(String token) {
        wireMockServer.stubFor(post(urlEqualTo("/auth"))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .withRequestBody(containing("token=" + token))
                .willReturn(aResponse().withStatus(200)));
    }

    @Step("Mock external service: authentication rejected (/auth -> {statusCode}) for token '{token}'")
    public void stubAuthFailure(String token, int code) {
        wireMockServer.stubFor(post(urlEqualTo("/auth"))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .withRequestBody(containing("token=" + token))
                .willReturn(aResponse().withStatus(code)));
    }

    @Step("Mock external service: successful action (/doAction -> 200 OK) for token '{token}'")
    public void stubActionSuccess(String token) {
        wireMockServer.stubFor(post(urlEqualTo("/doAction"))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .withRequestBody(containing("token=" + token))
                .willReturn(aResponse().withStatus(200)));
    }

    @Step("Mock external service: action failure (/doAction -> {statusCode}) for token '{token}'")
    public void stubActionFailed(String token, int code) {
        wireMockServer.stubFor(post(urlEqualTo("/doAction"))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .withRequestBody(containing("token=" + token))
                .willReturn(aResponse().withStatus(code)));
    }

    @Step("\"Verify external service received exactly {count} request(s) to '{url}'")
    public int getRequestCount(String url) {
        return wireMockServer.findAll(postRequestedFor(urlEqualTo(url))).size();
    }
}
