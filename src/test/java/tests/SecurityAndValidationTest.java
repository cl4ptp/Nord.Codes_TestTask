package tests;

import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

        assertEquals(403, response.getStatusCode(), "Login successful");
    }

    @Test
    @Story("API Key")
    @DisplayName("Reject access with invalid X-Api-Key")
    void invalidApiKeyTest() {
        String token = TokenGenerator.generateValidToken();
        Response response = apiClient.sendRequest(token, LOGIN.name(), "Invalid_key");

        assertEquals(403, response.getStatusCode(), "Login successful");
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
        assertEquals("ERROR", body.getResult(), "Login successful");
    }

    @Test
    @Story("Action validation")
    @DisplayName("Send unsupported action")
    void unknownActionTest() {
        String token = TokenGenerator.generateValidToken();

        Response response = apiClient.sendRequest(token, "UNSUPPORTED_ACTION");

        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("ERROR", body.getResult(), "Success send unexpected action");
    }
}
