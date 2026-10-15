package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.model.ApiResponse;
import org.utils.TokenGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.model.ActionType.*;
import static wiremock.com.github.jknack.handlebars.helper.ConditionalHelpers.or;

public class ActionTest extends BaseTest {

    @Test
    @Story("ACTION")
    @Description("Successfully action with authorized token")
    void actionAfterLoginSuccessTest() {
        String token = TokenGenerator.generateValidToken();
        mockService.stubAuthSuccess(token);
        mockService.stubActionSuccess(token);

        apiClient.sendRequest(token, LOGIN.name());
        Response response = apiClient.sendRequest(token, ACTION.name());

        assertEquals(200, response.getStatusCode(), "Expected HTTP 200 status code on successful ACTION request");
        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("OK", body.getResult(), "Expected result to be 'OK', but received error message: '" + body.getMessage() + "'");
    }

    @Test
    @Story("ACTION")
    @Description("Prohibition of action execution without prior LOGIN")
    void actionWithoutLoginForbiddenTest() {
        String token = TokenGenerator.generateValidToken();

        Response response = apiClient.sendRequest(token, ACTION.name());

        assertEquals(403, response.getStatusCode(), "Expected HTTP 403 Unauthorized status code on failure ACTION request");
        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("ERROR", body.getResult(), "Expected operation to be rejected with 'ERROR' for a non-authenticated token");
        assertEquals("Token '" + token + "' not found", body.getMessage(),
                "Expected notification that the token was not found, but received a different message: '" + body.getMessage() + "'");
        assertEquals(0, mockService.getRequestCount("/doAction"),
                "External service /doAction should not be called when token is unauthorized");
    }

    @Test
    @Story("ACTION")
    @Description("Unsuccessfully action after LOGOUT from service")
    void actionAfterLogoutFailedTest() {
        String token = TokenGenerator.generateValidToken();
        mockService.stubAuthSuccess(token);

        apiClient.sendRequest(token, LOGIN.name());
        apiClient.sendRequest(token, LOGOUT.name());
        Response response = apiClient.sendRequest(token, ACTION.name());

        assertEquals(403, response.getStatusCode(), "Expected HTTP 403 Unauthorized status code on failure ACTION request");
        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("ERROR", body.getResult(), "Expected operation to be rejected with 'ERROR' because session was terminated via LOGOUT");
        assertEquals("Token '" + token + "' not found", body.getMessage(),
                "Expected notification that the token was not found, but received a different message: '" + body.getMessage() + "'");
    }

    @Test
    @Story("ACTION")
    @Description("Unsuccessfully action with external service is down")
    void actionExternalFailureTest() {
        String token = TokenGenerator.generateValidToken();
        mockService.stubAuthSuccess(token);
        mockService.stubActionFaailed(token, 500);

        apiClient.sendRequest(token, LOGIN.name());
        Response response = apiClient.sendRequest(token, ACTION.name());

        assertEquals(500, response.getStatusCode(), "Expected HTTP 500 when external /doAction service responds with server error");
        ApiResponse body = response.as(ApiResponse.class);
        assertEquals("ERROR", body.getResult(), "Expected result to be 'ERROR' due to downstream /doAction failure");
        assertEquals("Internal Server Error", body.getMessage(),
                "Expected notification 'Internal Server Error', but received a different message: '" + body.getMessage() + "'");
    }
}
