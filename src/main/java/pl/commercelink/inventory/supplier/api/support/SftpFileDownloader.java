package pl.commercelink.inventory.supplier.api.support;

import com.jcraft.jsch.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class SftpFileDownloader {

    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public SftpFileDownloader(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public byte[] download(String remoteFilePath) throws ResourceDownloadException {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, host, port);
            session.setPassword(password);

            // Avoid asking for key confirmation
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            // Establish the SSH connection
            session.connect();

            // Create an SFTP channel
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

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
        }
    }
}
