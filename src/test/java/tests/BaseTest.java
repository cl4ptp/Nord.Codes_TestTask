package tests;

import mock.ExternalServiceMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class BaseTest {
    public static final String API_KEY = "qazWSXedc";
    public static final String BASE_URL = "http://localhost:8080";
    public static final int MOCK_PORT = 8888;
    public static ExternalServiceMock mockService;

    @BeforeAll
    static void setUpMock() {
        mockService = new ExternalServiceMock(MOCK_PORT);
        mockService.start();
    }

    @AfterAll
    static void cleanUpMock() {
        mockService.stop();
    }

    @BeforeEach
    void resetState() {
        mockService.reset();
    }
}
