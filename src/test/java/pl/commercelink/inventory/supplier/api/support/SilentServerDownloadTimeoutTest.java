package pl.commercelink.inventory.supplier.api.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A server that accepts the TCP connection but never sends its greeting (FTP reply / SSH version line).
 * Without timeouts the downloader would wait forever.
 */
class SilentServerDownloadTimeoutTest {

    private static final Duration SHORT = Duration.ofMillis(300);

    private ServerSocket silentServer;

    @BeforeEach
    void setUp() throws IOException {
        silentServer = new ServerSocket(0);
    }

    @AfterEach
    void tearDown() throws IOException {
        silentServer.close();
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void ftpDownloadFailsWhenServerNeverGreets() {
        FtpFileDownloader downloader = new FtpFileDownloader(
                "127.0.0.1", silentServer.getLocalPort(), "user", "pass", SHORT, SHORT);

        assertThrows(ResourceDownloadException.class, () -> downloader.download("/feed.csv"));
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void sftpDownloadFailsWhenServerNeverGreets() {
        SftpFileDownloader downloader = new SftpFileDownloader(
                "127.0.0.1", silentServer.getLocalPort(), "user", "pass", SHORT, SHORT);

        assertThrows(ResourceDownloadException.class, () -> downloader.download("/feed.csv"));
    }
}
