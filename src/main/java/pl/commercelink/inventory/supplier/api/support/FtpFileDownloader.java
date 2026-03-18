package pl.commercelink.inventory.supplier.api.support;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FtpFileDownloader {

    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public FtpFileDownloader(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public byte[] download(String remoteFilePath) throws ResourceDownloadException {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(host, port);
            ftpClient.login(username, password);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            try (InputStream inputStream = ftpClient.retrieveFileStream(remoteFilePath);
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                ftpClient.completePendingCommand();
                System.out.println("File downloaded successfully!");
                return outputStream.toByteArray();
            }

        } catch (IOException e) {
            throw new ResourceDownloadException("Failed to download resource from FTP server.", e);
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}