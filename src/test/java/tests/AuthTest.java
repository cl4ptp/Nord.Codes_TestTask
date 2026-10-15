package tests;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.model.ApiResponse;
import org.utils.TokenGenerator;

import static io.qameta.allure.Allure.step;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.model.ActionType.*;

@Feature("Authentication user('LOGIN/LOGOUT')")
public class AuthTest extends BaseTest {

    @Test
    @Story("LOGIN")
    @DisplayName("Successfully login")
    void successfulLoginTest() {
        String token = TokenGenerator.generateValidToken();

        step("1. Set up mock: stub successful external authentication (/auth -> 200 OK)", () ->
                mockService.stubAuthSuccess(token)
        );

        Response response = step("2. Send LOGIN request with valid token", () ->
                apiClient.sendRequest(token, LOGIN.name())
        );

        step("3. Verify response status 200 OK and result 'OK'", () -> {
            assertEquals(200, response.getStatusCode(), "Expected HTTP 200 status code on successful LOGIN request");
            ApiResponse body = response.as(ApiResponse.class);
            assertEquals("OK", body.getResult(),
                    () -> "Expected result to be 'OK', but received message: " + body.getMessage());
        });

        step("4. Verify external service /auth received exactly 1 request", () ->
                assertEquals(1, mockService.getRequestCount("/auth"), "External service /auth should be called exactly once")
        );
    }

    @Test
    @Story("LOGIN")
    @DisplayName("Unsuccessfully external service login")
    @Disabled("BUG: Downstream 401 translated to 500")
    void loginExternalAuthFailed() {
        String token = TokenGenerator.generateValidToken();

        step("1. Set up mock: stub authentication failure (/auth -> 401 Unauthorized)", () ->
                mockService.stubAuthFailure(token, 401)
        );

        Response response = step("2. Send LOGIN request with rejected token", () ->
                apiClient.sendRequest(token, LOGIN.name())
        );

        step("3. Verify response status and error body", () -> {
            // TODO: BUG, Service should return 401 Unauthorized with meaningful error context instead of generic 500 "Internal service error"
            assertEquals(401, response.getStatusCode(), "Expected HTTP 401 Unauthorized when external auth service responds with an error");
            ApiResponse body = response.as(ApiResponse.class);
            assertEquals("ERROR", body.getResult(), "Expected result field to be 'ERROR' after external auth rejection");
        });
    }

    @Test
    @Story("LOGOUT")
    @DisplayName("Successfully logout")
    void successfullyLogout() {
        String token = TokenGenerator.generateValidToken();

        step("1. Set up mock: stub successful external authentication", () ->
                mockService.stubAuthSuccess(token)
        );

        step("2. Authorize user session via LOGIN", () -> {
            Response loginResponse = apiClient.sendRequest(token, LOGIN.name());
            assertEquals(200, loginResponse.getStatusCode(), "Expected 200 OK on LOGIN");
        });

        Response response = step("3. Send LOGOUT request to terminate session", () ->
                apiClient.sendRequest(token, LOGOUT.name())
        );

        step("4. Verify response status 200 OK and session termination", () -> {
            assertEquals(200, response.getStatusCode(), "Expected HTTP 200 status code on successful LOGOUT request");
            ApiResponse body = response.as(ApiResponse.class);
            assertEquals("OK", body.getResult(),
                    () -> "Expected session to be terminated with result 'OK', but received: '" + body.getMessage() + "'");
        });
    }

    @Test
    @Story("LOGIN")
    @DisplayName("Reject duplicate login with an already active token")
    void duplicateLoginTest() {
        String token = TokenGenerator.generateValidToken();

        step("1. Set up mock: stub successful external authentication", () ->
                mockService.stubAuthSuccess(token)
        );

        step("2. Perform initial LOGIN with token", () -> {
            Response firstLogin = apiClient.sendRequest(token, LOGIN.name());
            assertEquals(200, firstLogin.getStatusCode(), "Initial login must succeed");
        });

        Response secondLoginResponse = step("3. Send duplicate LOGIN request with the same active token", () ->
                apiClient.sendRequest(token, LOGIN.name())
        );

        step("4. Verify duplicate login is rejected with HTTP 409 Conflict and 'Token already exists'", () -> {
            assertEquals(409, secondLoginResponse.getStatusCode(), "Duplicate login must return HTTP 409 Conflict");
            ApiResponse body = secondLoginResponse.as(ApiResponse.class);
            assertEquals("ERROR", body.getResult(), "Expected result to be 'ERROR' for duplicate login");
            assertEquals("Token '" + token + "' already exists", body.getMessage(),
                    () -> "Expected notification that the token already exists, but received: '" + body.getMessage() + "'");
        });

        step("5. Verify external service /auth was called only once", () ->
                assertEquals(1, mockService.getRequestCount("/auth"), "External service /auth should be called exactly once")
        );
    }

    @Test
    @Story("LOGOUT")
    @DisplayName("Logout without valid login")
    void logoutWithoutLoginTest() {
        String token = TokenGenerator.generateValidToken();

        Response response = step("1. Send LOGOUT request for an unregistered token", () ->
                apiClient.sendRequest(token, LOGOUT.name())
        );

        step("2. Verify logout is rejected with HTTP 403 Forbidden and 'Token not found'", () -> {
            assertEquals(403, response.getStatusCode(), "Expected HTTP 403 Forbidden status code on failure LOGOUT request");
            ApiResponse body = response.as(ApiResponse.class);
            assertEquals("ERROR", body.getResult(), "Expected operation to be rejected with 'ERROR' for unregistered token");
            assertEquals("Token '" + token + "' not found", body.getMessage(),
                    () -> "Expected notification that the token was not found, but received: '" + body.getMessage() + "'");
        });
    }
}