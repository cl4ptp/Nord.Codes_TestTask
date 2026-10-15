package tests;

import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.model.ApiResponse;
import org.utils.TokenGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.model.ActionType.*;

public class AuthTest extends BaseTest {

    @Test
    @Story("LOGIN")
    @DisplayName("Successfully login")
    void successfulLoginTest() {
        String token = TokenGenerator.generateValidToken();
        mockService.stubAuthSuccess(token);

        Response response = apiClient.sendRequest(token, LOGIN.name());

        assertEquals(200, response.getStatusCode(), "Expected HTTP 200 status code on successful LOGIN request");
        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("OK", body.getResult(), "Expected result to be 'OK', but received message:" + body.getMessage());
    }

    @Test
    @Story("LOGIN")
    @DisplayName("Unsuccessfully external service login")
    @Disabled("BUG: Downstream 401 translated to 500")
    void loginExternalAuthFailed() {
        String token = TokenGenerator.generateValidToken();
        mockService.stubAuthFailure(token, 401);

        Response response = apiClient.sendRequest(token, LOGIN.name());
        assertEquals(401, response.getStatusCode(), "Expected HTTP 401 Unauthorized when external auth service responds with an error");
        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("ERROR", body.getResult(), "Expected result field to be 'ERROR' after external auth rejection");
    }

    @Test
    @Story("LOGOUT")
    @DisplayName("Successfully logout")
    void successfullyLogout() {
        String token = TokenGenerator.generateValidToken();
        mockService.stubAuthSuccess(token);

        apiClient.sendRequest(token, LOGIN.name());
        Response response = apiClient.sendRequest(token, LOGOUT.name());

        assertEquals(200, response.getStatusCode(), "Expected HTTP 200 status code on successful LOGOUT request");
        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("OK", body.getResult(), "Expected session to be terminated with result 'OK', but received: '" + body.getMessage() + "'");
    }

    @Test
    @Story("LOGIN")
    @DisplayName("Reject duplicate login with an already active token")
    void duplicateLoginTest() {
        String token = TokenGenerator.generateValidToken();
        mockService.stubAuthSuccess(token);

        apiClient.sendRequest(token, LOGIN.name());
        Response secondLoginResponse = apiClient.sendRequest(token, LOGIN.name());

        assertEquals(409, secondLoginResponse.getStatusCode(), "Duplicate login must return HTTP 409 Conflict");
        ApiResponse body = secondLoginResponse.as(ApiResponse.class);
        assertEquals("ERROR", body.getResult());
        assertEquals("Token '" + token + "' already exists", body.getMessage(),
                "Expected notification that the token was already exists, but received a different message: '" + body.getMessage() + "'");
        assertEquals(1, mockService.getRequestCount("/auth"), "External service /auth should be called exactly once");
    }

    @Test
    @Story("LOGOUT")
    @DisplayName("Logout without valid login")
    void logoutWithoutLoginTest() {
        String token = TokenGenerator.generateValidToken();

        Response response = apiClient.sendRequest(token, LOGOUT.name());

        assertEquals(403, response.getStatusCode(), "Expected HTTP 403 Forbidden status code on failure LOGOUT request");
        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("ERROR", body.getResult(), "Expected operation to be rejected with 'ERROR' because session was terminated via LOGOUT");
        assertEquals("Token '" + token + "' not found", body.getMessage(),
                "Expected notification that the token was not found, but received a different message: '" + body.getMessage() + "'");
    }
}
