package tests;

import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.model.ApiResponse;
import org.utils.TokenGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthTest extends BaseTest {

    @Test
    @Story("LOGIN")
    @DisplayName("Successfully login")
    void successfulLoginTest() {
        String token = TokenGenerator.generateValidToken();
        mockService.stubAuthSuccess(token);

        Response response = apiClient.sendRequest(token, "LOGIN");

        assertEquals(200, response.getStatusCode(), "Login failed");
        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("OK", body.getResult(), "Login in external service successfully, but error messaage in body = " + body.getMessage());
    }

    @Test
    @Story("LOGIN")
    @DisplayName("Unsuccessfully external service login")
    void loginExternaalAuthFailed() {
        String token = TokenGenerator.generateValidToken();
        mockService.stubAuthFailure(token, 401);

        Response response = apiClient.sendRequest(token, "LOGIN");

        assertEquals(401, response.getStatusCode(), "Login successful, but should be failed");
        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("ERROR", body.getResult(), "Login in external service successful, but should be failed");
    }

    @Test
    @Story("LOGOUT")
    @DisplayName("Successfully logout")
    void successfullyLogout() {
        String token = TokenGenerator.generateValidToken();
        mockService.stubAuthSuccess(token);

        apiClient.sendRequest(token, "LOGIN");
        Response response = apiClient.sendRequest(token, "LOGOUT");

        assertEquals(200, response.getStatusCode(), "Logout failed");
        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("OK", body.getResult(), "Logout from external service successfully, but error message in body = " + body.getMessage());
    }
}
