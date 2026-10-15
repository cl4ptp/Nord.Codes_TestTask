package tests;

import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.model.ApiResponse;
import org.utils.TokenGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.model.ActionType.LOGIN;

public class SecurityAndValidationTest extends BaseTest {

    @Test
    @Story("API Key")
    @DisplayName("Reject access without X-Api-Key")
    void missingApiKeyTest() {
        String token = TokenGenerator.generateValidToken();
        Response response = apiClient.sendRequest(token, LOGIN.name(), null);

        assertEquals(401, response.getStatusCode(), "Expected error 401 Unauthorized when X-Api-Key header is missing");
        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("ERROR", body.getResult(), "Expected result to be 'ERROR' due to validation failure");
        assertEquals("Missing or invalid API Key", body.getMessage(), "Expected 'Missing or invalid API Key' message for missing API key, but received: '" + body.getMessage() + "'");
    }

    @Test
    @Story("API Key")
    @DisplayName("Reject access with invalid X-Api-Key")
    void invalidApiKeyTest() {
        String token = TokenGenerator.generateValidToken();
        Response response = apiClient.sendRequest(token, LOGIN.name(), "Invalid_key");

        assertEquals(401, response.getStatusCode(), "Expected error 401 Unauthorized when X-Api-Key header is wrong");
        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("ERROR", body.getResult(), "Expected result to be 'ERROR' due to validation failure");
        assertEquals("Missing or invalid API Key", body.getMessage(), "Expected 'Missing or invalid API Key' message for invalid API key, but received: '" + body.getMessage() + "'");
    }

    @ParameterizedTest
    @Story("Token Validation")
    @DisplayName("CHeck token format(32 chars, A-Z, 0-9)")
    @ValueSource(strings = {
            "SHORTTOKEN",
            "TOKENLONGERTHAN32CHARACTERS1234567",
            "!@#1234567890ABCDEFGHIJKLMNOPQRS",
            "abcedfghijklmnopqrstuvwxyz012345",
            "TOKENWITH31CHARACTER01234567890",
            "TOKENWITH33CHARACTER0123456789012"
    })
    void invalidTokenFormatsTest(String invalidToken) {
        Response response = apiClient.sendRequest(invalidToken, LOGIN.name());

        assertEquals(400, response.getStatusCode(), "Validation errors must return HTTP 400 Bad Request");
        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("ERROR", body.getResult(), "Expected error when token is wrong");
        assertEquals("token: должно соответствовать \"^[0-9A-F]{32}$\"", body.getMessage(),
                "Expected notification that the token has wrong, but received a different message: '" + body.getMessage() + "'");
    }

    @ParameterizedTest
    @Story("Action validation")
    @DisplayName("Send unsupported action")
    @ValueSource(strings = {
            "UNSUPPORTED_ACTION",
            ""
    })
    void unknownActionTest(String action) {
        String token = TokenGenerator.generateValidToken();

        Response response = apiClient.sendRequest(token, action);

        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("ERROR", body.getResult(), "Expected error when send unexpected action");
        assertTrue(body.getMessage().contains("action: invalid action"),
                "Expected notification that the action has wrong, but received a different message: '" + body.getMessage() + "'");
    }

    @Test
    @Story("Token Validation")
    @Tag("BUG")
    @DisplayName("Check support token with characters G-Z")
    void testTokenWithCharsBeyondHex() {
        String tokenWithGtoZ = "123456789012345678901234567890GZ";
        mockService.stubAuthSuccess(tokenWithGtoZ);

        Response response = apiClient.sendRequest(tokenWithGtoZ, LOGIN.name());

        assertEquals(200, response.getStatusCode(), "App fails to accept valid characters G-Z");
    }

    @Test
    @Story("Token Validation")
    @DisplayName("Reject access without token")
    void missingTokenTest() {
        Response response = apiClient.sendRequest(null, LOGIN.name());

        assertEquals(400, response.getStatusCode(), "Expected error 400 when token is missing");
        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("ERROR", body.getResult(), "Expected result to be 'ERROR' due to validation failure");
        assertEquals("token: не должно равняться null", body.getMessage(),
                "Expected 'token: не должно равняться null' message for missing token, but received: '" + body.getMessage() + "'");
    }

    @Test
    @Story("Token and Action Validation")
    @DisplayName("Reject access without action and token")
    void missingTokenAndActionTest() {
        Response response = apiClient.sendRequest(null, null);

        assertEquals(400, response.getStatusCode(), "Expected error 400 when token and action is missing");
        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("ERROR", body.getResult(), "Expected result to be 'ERROR' due to validation failure");
        assertTrue(body.getMessage().contains("action: invalid action 'null'. Allowed: LOGIN, LOGOUT, ACTION"),
                "Expected response message to contain validation error for missing action, but received: '\" + body.getMessage() + \"'");
        assertTrue(body.getMessage().contains("token: не должно равняться null"),
                "Expected response message to contain validation error for missing token, but received: '\" + body.getMessage() + \"'");
    }
}
