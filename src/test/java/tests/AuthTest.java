package tests;

import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
    void loginExternaalAuthFailed() {
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
        assertEquals("OK", body.getResult(), "Expected session to be terminated with result 'OK', but received: " + body.getMessage());
    }
}
