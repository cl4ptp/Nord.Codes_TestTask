package org.client;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

public class ApiClient {
    private final String baseUrl;
    private final String defaultApiKey;

    public ApiClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.defaultApiKey = apiKey;
    }

    public Response sendRequest(String token, String action, String apiKey) {
        Map<String, String> formParams = new HashMap<>();
        if (token != null) formParams.put("token", token);
        if (action != null) formParams.put("action", action);

        var requestSpecification = RestAssured.given()
                .filter(new AllureRestAssured())
                .contentType(ContentType.URLENC)
                .accept(ContentType.JSON)
                .formParams(formParams);

        if (apiKey != null) {
            requestSpecification.header("X-Api-Key", apiKey);
        }

        return requestSpecification.post("/endpoint");
    }

    public Response sendRequest(String token, String action) {
        return sendRequest(token, action, defaultApiKey);
    }
}
