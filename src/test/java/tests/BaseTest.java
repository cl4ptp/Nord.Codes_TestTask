package tests;

import org.client.ApiClient;
import org.mock.ExternalServiceMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class BaseTest {
    public static final String API_KEY = "qazWSXedc";
    public static final String BASE_URL = "http://localhost:8080";
    public static final int MOCK_PORT = 8888;
    public static final int APP_PORT = 8080;

    private static final String APP_JAR_PATH = "internal-0.0.1-SNAPSHOT.jar";

    public static ExternalServiceMock mockService;
    public static ApiClient apiClient;
    private static Process appProcess;

    @BeforeAll
    static void startSuite() throws Exception {
        mockService = new ExternalServiceMock(MOCK_PORT);
        mockService.start();

        killProcessOnPort(APP_PORT);

        ProcessBuilder processBuilder = new ProcessBuilder(
                "java",
                "-jar",
                "-Dsecret=" + API_KEY,
                "-Dmock=http://localhost:" + MOCK_PORT + "/",
                APP_JAR_PATH
        );

        processBuilder.inheritIO();
        appProcess = processBuilder.start();

        waitForPort(APP_PORT, 15);

        apiClient = new ApiClient(BASE_URL, API_KEY);
    }

    @AfterAll
    static void stopSuite() {
        if (appProcess != null && appProcess.isAlive()) {
            appProcess.destroyForcibly();
        }

        if (mockService != null) {
            mockService.stop();
        }
    }

    @BeforeEach
    void resetState() {
        mockService.reset();
    }

    private static void killProcessOnPort(int port) {
        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            if (isWindows) {
                new ProcessBuilder("cmd.exe", "/c", "for /f \"tokens=5\" %a in ('netstat -aon ^| find \":" + port + "\" ^| find \"LISTENING\"') do taskkill /f /pid %a")
                        .inheritIO()
                        .start()
                        .waitFor(3, TimeUnit.SECONDS);
            } else {
                new ProcessBuilder("bash", "-c", "kill -9 $(lsof -t -i:" + port + ")")
                        .inheritIO()
                        .start()
                        .waitFor(3, TimeUnit.SECONDS);
            }
        } catch (Exception ignored) {}
    }

    private static void waitForPort(int port, int timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        while (System.currentTimeMillis() < deadline) {
            try (Socket socket = new Socket("localhost", port)) {
                return;
            } catch (IOException ignored) {
                Thread.sleep(300);
            }
        }
        throw new IllegalStateException("Application failed to start within " + timeoutSeconds + " seconds on port " + port);
    }
}
