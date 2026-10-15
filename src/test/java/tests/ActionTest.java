package tests;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.model.ApiResponse;
import org.utils.TokenGenerator;

import static io.qameta.allure.Allure.step;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.model.ActionType.*;

@Feature("Actions('ACTION')")
public class ActionTest extends BaseTest {

    @Test
    @Story("ACTION")
    @DisplayName("Successfully action with authorized token")
    @Description("Verifies that an authorized user can perform an action and downstream /doAction is executed")
    void actionAfterLoginSuccessTest() {
        String token = TokenGenerator.generateValidToken();

        step("1. Set up mock: stub successful external authentication and action", () -> {
            mockService.stubAuthSuccess(token);
            mockService.stubActionSuccess(token);
        });

        step("2. Authorize user session via LOGIN", () -> {
            Response loginResponse = apiClient.sendRequest(token, LOGIN.name());
            assertEquals(200, loginResponse.getStatusCode(), "Expected 200 OK on LOGIN");
        });

        Response response = step("3. Send ACTION request with authorized token", () ->
                apiClient.sendRequest(token, ACTION.name())
        );

        step("4. Verify action response and downstream invocation", () -> {
            assertEquals(200, response.getStatusCode(), "Expected HTTP 200 status code on successful ACTION request");
            ApiResponse body = response.as(ApiResponse.class);
            assertEquals("OK", body.getResult(),
                    () -> "Expected result to be 'OK', but received error message: '" + body.getMessage() + "'");
            assertEquals(1, mockService.getRequestCount("/doAction"),
                    "External service /doAction should be called exactly once");
        });
    }

    @Test
    @Story("ACTION")
    @DisplayName("Prohibition of action execution without prior LOGIN")
    @Description("Verifies that action cannot be performed by an unauthorized token")
    void actionWithoutLoginForbiddenTest() {
        String token = TokenGenerator.generateValidToken();

        Response response = step("1. Send ACTION request without prior LOGIN", () ->
                apiClient.sendRequest(token, ACTION.name())
        );

        step("2. Verify access is rejected with HTTP 403 Forbidden and 'Token not found'", () -> {
            assertEquals(403, response.getStatusCode(), "Expected HTTP 403 Forbidden status code on unauthorized ACTION request");
            ApiResponse body = response.as(ApiResponse.class);
            assertEquals("ERROR", body.getResult(), "Expected operation to be rejected with 'ERROR' for a non-authenticated token");
            assertEquals("Token '" + token + "' not found", body.getMessage(),
                    () -> "Expected notification that the token was not found, but received: '" + body.getMessage() + "'");
        });

        step("3. Verify external service /doAction was not called", () ->
                assertEquals(0, mockService.getRequestCount("/doAction"),
                        "External service /doAction should not be called when token is unauthorized")
        );
    }

    @Test
    @Story("ACTION")
    @DisplayName("Unsuccessfully action after LOGOUT from service")
    @Description("Verifies that action cannot be performed after token was invalidated via LOGOUT")
    void actionAfterLogoutFailedTest() {
        String token = TokenGenerator.generateValidToken();

        step("1. Set up mock: stub successful external authentication", () ->
                mockService.stubAuthSuccess(token)
        );

        step("2. Authorize user session via LOGIN", () -> {
            Response loginResponse = apiClient.sendRequest(token, LOGIN.name());
            assertEquals(200, loginResponse.getStatusCode(), "Expected 200 OK on LOGIN");
        });

        step("3. Terminate user session via LOGOUT", () -> {
            Response logoutResponse = apiClient.sendRequest(token, LOGOUT.name());
            assertEquals(200, logoutResponse.getStatusCode(), "Expected 200 OK on LOGOUT");
        });

        Response response = step("4. Send ACTION request with invalidated token", () ->
                apiClient.sendRequest(token, ACTION.name())
        );

        step("5. Verify access is rejected with HTTP 403 Forbidden and 'Token not found'", () -> {
            assertEquals(403, response.getStatusCode(), "Expected HTTP 403 Forbidden status code on terminated session ACTION request");
            ApiResponse body = response.as(ApiResponse.class);
            assertEquals("ERROR", body.getResult(), "Expected operation to be rejected with 'ERROR' because session was terminated via LOGOUT");
            assertEquals("Token '" + token + "' not found", body.getMessage(),
                    () -> "Expected notification that the token was not found, but received: '" + body.getMessage() + "'");
        });

        step("6. Verify external service /doAction was not called", () ->
                assertEquals(0, mockService.getRequestCount("/doAction"),
                        "External service /doAction should not be called after logout")
        );
    }

    @Test
    @Story("ACTION")
    @DisplayName("Unsuccessfully action with external service is down")
    @Description("Verifies error handling when external service /doAction responds with HTTP 500")
    void actionExternalFailureTest() {
        String token = TokenGenerator.generateValidToken();

        step("1. Set up mock: stub successful auth and failing external action (HTTP 500)", () -> {
            mockService.stubAuthSuccess(token);
            mockService.stubActionFailed(token, 500);
        });

        step("2. Authorize user session via LOGIN", () -> {
            Response loginResponse = apiClient.sendRequest(token, LOGIN.name());
            assertEquals(200, loginResponse.getStatusCode(), "Expected 200 OK on LOGIN");
        });

        Response response = step("3. Send ACTION request encountering downstream failure", () ->
                apiClient.sendRequest(token, ACTION.name())
        );

        step("4. Verify response is HTTP 500 with 'Internal Server Error'", () -> {
            assertEquals(500, response.getStatusCode(), "Expected HTTP 500 when external /doAction service responds with server error");
            ApiResponse body = response.as(ApiResponse.class);
            assertEquals("ERROR", body.getResult(), "Expected result to be 'ERROR' due to downstream /doAction failure");
            assertEquals("Internal Server Error", body.getMessage(),
                    () -> "Expected notification 'Internal Server Error', but received: '" + body.getMessage() + "'");
        });
    }
}