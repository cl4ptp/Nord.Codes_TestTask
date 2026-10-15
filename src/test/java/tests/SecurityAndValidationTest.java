package tests;

import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.model.ApiResponse;
import org.utils.TokenGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals("ERROR", body.getResult(), "Expected result to be 'ERROR' due to downstream /auth failure");
        assertEquals("Missing or invalid API Key", body.getMessage(), "Expected message to be 'Missing or invalid API Key' due to downstream /auth failure");
    }

    @Test
    @Story("API Key")
    @DisplayName("Reject access with invalid X-Api-Key")
    void invalidApiKeyTest() {
        String token = TokenGenerator.generateValidToken();
        Response response = apiClient.sendRequest(token, LOGIN.name(), "Invalid_key");

        assertEquals(401, response.getStatusCode(), "Expected error 401 Unauthorized when X-Api-Key header is wrong");
        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("ERROR", body.getResult(), "Expected result to be 'ERROR' due to downstream /auth failure");
        assertEquals("Missing or invalid API Key", body.getMessage(), "Expected message to be 'Missing or invalid API Key' due to downstream /auth failure");
    }

    @ParameterizedTest
    @Story("Token Validation")
    @DisplayName("CHeck token format(32 chars, A-Z, 0-9)")
    @ValueSource(strings = {
            "SHORTTOKEN",
            "TOKENLONGERTHAN32CHARACTERS1234567",
            "!@#1234567890ABCDEFGHIJKLMNOPQRS",
            "abcedfghijklmnopqrstuvwxyz012345"
    })
    void InvalidTokenFormatsTest(String invalidToken) {
        Response response = apiClient.sendRequest(invalidToken, LOGIN.name());

        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("ERROR", body.getResult(), "Expected error when token is wrong");
        assertEquals("token: должно соответствовать \"^[0-9A-F]{32}$\"", body.getMessage(),
                "Expected notification that the token has wrong, but received a different message: '" + body.getMessage() + "'");
    }

    @Test
    @Story("Action validation")
    @DisplayName("Send unsupported action")
    void unknownActionTest() {
        String token = TokenGenerator.generateValidToken();

        Response response = apiClient.sendRequest(token, "UNSUPPORTED_ACTION");

        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("ERROR", body.getResult(), "Expected error when send unexpected action");
        assertEquals("action: invalid action 'UNSUPPORTED_ACTION'. Allowed: LOGIN, LOGOUT, ACTION", body.getMessage(),
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
}
