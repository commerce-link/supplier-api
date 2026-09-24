package pl.commercelink.inventory.supplier.api.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpFileDownloaderTest {

    private static final Duration SHORT = Duration.ofMillis(300);

    private final CountDownLatch release = new CountDownLatch(1);
    private ServerSocket server;

    @AfterEach
    void tearDown() throws IOException {
        release.countDown();
        server.close();
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void failsWhenServerStopsSendingTheBodyMidway() throws IOException {
        // given
        String url = serve((path, out) -> {
            write(out, "HTTP/1.1 200 OK\r\nContent-Length: 1000\r\n\r\npartial");
            awaitRelease();
        });

        // when / then
        assertThrows(ResourceDownloadException.class,
                () -> new HttpFileDownloader(SHORT, SHORT).downloadResource(url));
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void failsWhenServerNeverAnswers() throws IOException {
        // given
        String url = serve((path, out) -> awaitRelease());

        // when / then
        assertThrows(ResourceDownloadException.class,
                () -> new HttpFileDownloader(SHORT, SHORT).downloadResource(url));
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void followsRedirectAndDownloadsTheBody() throws Exception {
        // given
        String url = serve((path, out) -> {
            if (path.equals("/feed.csv")) {
                write(out, "HTTP/1.1 302 Found\r\nLocation: http://127.0.0.1:" + server.getLocalPort()
                        + "/moved.csv\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");
            } else {
                write(out, "HTTP/1.1 200 OK\r\nContent-Length: 7\r\nConnection: close\r\n\r\nsku;qty");
            }
        });

        // when
        byte[] bytes = new HttpFileDownloader(SHORT, SHORT).downloadResource(url);

        // then
        assertArrayEquals("sku;qty".getBytes(StandardCharsets.UTF_8), bytes);
    }

    private String serve(BiConsumer<String, OutputStream> handler) throws IOException {
        server = new ServerSocket(0);
        Thread thread = new Thread(() -> {
            while (!server.isClosed()) {
                try (Socket socket = server.accept()) {
                    handler.accept(readRequestPath(socket.getInputStream()), socket.getOutputStream());
                } catch (IOException ignored) {
                    // server closed by tearDown
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
        return "http://127.0.0.1:" + server.getLocalPort() + "/feed.csv";
    }

    private static String readRequestPath(InputStream in) throws IOException {
        StringBuilder head = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            head.append((char) b);
            if (head.toString().endsWith("\r\n\r\n")) {
                break;
            }
        }
        return head.toString().split(" ")[1];
    }

    private static void write(OutputStream out, String response) {
        try {
            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void awaitRelease() {
        try {
            release.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
