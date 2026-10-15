package tests;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.model.ApiResponse;
import org.utils.TokenGenerator;

import static io.qameta.allure.Allure.step;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.model.ActionType.LOGIN;

@Feature("Check access and validate data")
public class SecurityAndValidationTest extends BaseTest {

    @Test
    @Story("API Key")
    @DisplayName("Reject access without X-Api-Key")
    void missingApiKeyTest() {
        String token = TokenGenerator.generateValidToken();

        Response response = step("1. Send request without X-Api-Key header", () ->
                apiClient.sendRequest(token, LOGIN.name(), null)
        );

        step("2. Verify access is rejected with HTTP 401 Unauthorized and 'Missing or invalid API Key'", () -> {
            assertEquals(401, response.getStatusCode(), "Expected error 401 Unauthorized when X-Api-Key header is missing");
            ApiResponse body = response.as(ApiResponse.class);
            assertEquals("ERROR", body.getResult(), "Expected result to be 'ERROR' due to missing API key");
            assertEquals("Missing or invalid API Key", body.getMessage(),
                    () -> "Expected 'Missing or invalid API Key' message, but received: '" + body.getMessage() + "'");
        });
    }

    @Test
    @Story("API Key")
    @DisplayName("Reject access with invalid X-Api-Key")
    void invalidApiKeyTest() {
        String token = TokenGenerator.generateValidToken();

        Response response = step("1. Send request with invalid X-Api-Key header", () ->
                apiClient.sendRequest(token, LOGIN.name(), "Invalid_key")
        );

        step("2. Verify access is rejected with HTTP 401 Unauthorized and 'Missing or invalid API Key'", () -> {
            assertEquals(401, response.getStatusCode(), "Expected error 401 Unauthorized when X-Api-Key header is wrong");
            ApiResponse body = response.as(ApiResponse.class);
            assertEquals("ERROR", body.getResult(), "Expected result to be 'ERROR' due to invalid API key");
            assertEquals("Missing or invalid API Key", body.getMessage(),
                    () -> "Expected 'Missing or invalid API Key' message, but received: '" + body.getMessage() + "'");
        });
    }

    @ParameterizedTest(name = "Invalid token format: {0}")
    @Story("Token Validation")
    @DisplayName("Check token format (32 chars, A-Z, 0-9)")
    @ValueSource(strings = {
            "SHORTTOKEN",
            "TOKENLONGERTHAN32CHARACTERS1234567",
            "!@#1234567890ABCDEFGHIJKLMNOPQRS",
            "abcedfghijklmnopqrstuvwxyz012345",
            "TOKENWITH31CHARACTER01234567890",
            "TOKENWITH33CHARACTER0123456789012"
    })
    void invalidTokenFormatsTest(String invalidToken) {
        Response response = step("1. Send LOGIN request with invalid token format: '" + invalidToken + "'", () ->
                apiClient.sendRequest(invalidToken, LOGIN.name())
        );

        step("2. Verify validation fails with HTTP 400 Bad Request and regex constraint message", () -> {
            assertEquals(400, response.getStatusCode(), "Validation errors must return HTTP 400 Bad Request");
            ApiResponse body = response.as(ApiResponse.class);
            assertEquals("ERROR", body.getResult(), "Expected result to be 'ERROR' when token format is invalid");
            assertEquals("token: должно соответствовать \"^[0-9A-F]{32}$\"", body.getMessage(),
                    () -> "Expected regex violation message, but received: '" + body.getMessage() + "'");
        });
    }

    @ParameterizedTest(name = "Unsupported action: ''{0}''")
    @Story("Action validation")
    @DisplayName("Send unsupported action")
    @ValueSource(strings = {
            "UNSUPPORTED_ACTION",
            ""
    })
    void unknownActionTest(String action) {
        String token = TokenGenerator.generateValidToken();

        Response response = step("1. Send request with unsupported action: '" + action + "'", () ->
                apiClient.sendRequest(token, action)
        );

        step("2. Verify validation fails with HTTP 400 Bad Request and allowed actions message", () -> {
            assertEquals(400, response.getStatusCode(), "Expected HTTP 400 Bad Request for unsupported action");
            ApiResponse body = response.as(ApiResponse.class);
            assertEquals("ERROR", body.getResult(), "Expected result to be 'ERROR' when sending unexpected action");
            assertTrue(body.getMessage().contains("action: invalid action"),
                    () -> "Expected notification regarding invalid action, but received: '" + body.getMessage() + "'");
        });
    }

    @Test
    @Story("Token Validation")
    @Tag("BUG")
    @DisplayName("Check support token with characters G-Z")
    @Disabled("Known bug: token regex restricts to HEX (see Issue #1)")
    void testTokenWithCharsBeyondHex() {
        String tokenWithGtoZ = "123456789012345678901234567890GZ";

        step("1. Set up mock: stub successful external authentication for token with G-Z chars", () ->
                mockService.stubAuthSuccess(tokenWithGtoZ)
        );

        Response response = step("2. Send LOGIN request with token containing characters outside HEX (G-Z)", () ->
                apiClient.sendRequest(tokenWithGtoZ, LOGIN.name())
        );

        step("3. Verify application accepts token per specification", () ->
                assertEquals(200, response.getStatusCode(), "App fails to accept valid characters G-Z per specification")
        );
    }

    @Test
    @Story("Token Validation")
    @DisplayName("Reject access without token")
    void missingTokenTest() {
        Response response = step("1. Send request with null token", () ->
                apiClient.sendRequest(null, LOGIN.name())
        );

        step("2. Verify validation fails with HTTP 400 Bad Request and 'token: не должно равняться null'", () -> {
            assertEquals(400, response.getStatusCode(), "Expected error 400 when token is missing");
            ApiResponse body = response.as(ApiResponse.class);
            assertEquals("ERROR", body.getResult(), "Expected result to be 'ERROR' due to missing token");
            assertEquals("token: не должно равняться null", body.getMessage(),
                    () -> "Expected 'token: не должно равняться null' message, but received: '" + body.getMessage() + "'");
        });
    }

    @Test
    @Story("Token and Action Validation")
    @DisplayName("Reject access without action and token")
    void missingTokenAndActionTest() {
        Response response = step("1. Send request missing both token and action", () ->
                apiClient.sendRequest(null, null)
        );

        step("2. Verify validation fails with HTTP 400 Bad Request and validation errors for both fields", () -> {
            assertEquals(400, response.getStatusCode(), "Expected error 400 when token and action are missing");
            ApiResponse body = response.as(ApiResponse.class);
            assertEquals("ERROR", body.getResult(), "Expected result to be 'ERROR' due to validation failure");
            assertTrue(body.getMessage().contains("action: invalid action 'null'. Allowed: LOGIN, LOGOUT, ACTION"),
                    () -> "Expected message to contain action validation error, but received: '" + body.getMessage() + "'");
            assertTrue(body.getMessage().contains("token: не должно равняться null"),
                    () -> "Expected message to contain token validation error, but received: '" + body.getMessage() + "'");
        });
    }
}