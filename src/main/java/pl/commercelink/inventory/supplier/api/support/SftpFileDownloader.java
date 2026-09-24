package pl.commercelink.inventory.supplier.api.support;

import com.jcraft.jsch.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;

public class SftpFileDownloader {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final Duration connectTimeout;
    private final Duration readTimeout;

    public SftpFileDownloader(String host, int port, String username, String password) {
        this(host, port, username, password,
                HttpFileDownloader.DEFAULT_CONNECT_TIMEOUT, HttpFileDownloader.DEFAULT_READ_TIMEOUT);
    }

    /**
     * @param readTimeout maximum silence on the SSH connection, not a limit on the whole download
     */
    public SftpFileDownloader(String host, int port, String username, String password,
                              Duration connectTimeout, Duration readTimeout) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    public byte[] download(String remoteFilePath) throws ResourceDownloadException {
        Session session = null;
        ChannelSftp channelSftp = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            // Avoid asking for key confirmation
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            // Establish the SSH connection; the socket timeout also bounds silence during the transfer
            session.setTimeout(Math.toIntExact(readTimeout.toMillis()));
            session.connect(Math.toIntExact(connectTimeout.toMillis()));

            // Create an SFTP channel
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(Math.toIntExact(connectTimeout.toMillis()));

            // Download the file into memory
            try (InputStream inputStream = channelSftp.get(remoteFilePath);
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                System.out.println("File downloaded successfully!");
                return outputStream.toByteArray();
            }

        } catch (JSchException | SftpException | java.io.IOException e) {
            throw new ResourceDownloadException("Failed to download resource from sftp server.", e);
        } finally {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }
}
