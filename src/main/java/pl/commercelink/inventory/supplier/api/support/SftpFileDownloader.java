package pl.commercelink.inventory.supplier.api.support;

import com.jcraft.jsch.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
        return withChannel("Failed to download resource from sftp server.", channel -> {
            byte[] content = read(channel, remoteFilePath);
            System.out.println("File downloaded successfully!");
            return content;
        });
    }

    /**
     * Downloads the most recently modified file in {@code directory} whose whole name matches {@code fileName};
     * files modified in the same second are ordered by name.
     */
    public byte[] downloadLatest(String directory, Pattern fileName) throws ResourceDownloadException {
        return withChannel("Failed to download the latest file from sftp server.", channel -> {
            channel.cd(directory);
            String latest = RemoteFile.latest(list(channel), fileName)
                    .orElseThrow(() -> new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE,
                            "No file matching " + fileName.pattern() + " in " + directory));
            return read(channel, latest);
        });
    }

    private <T> T withChannel(String failureMessage, SftpOperation<T> operation) throws ResourceDownloadException {
        Session session = null;
        ChannelSftp channelSftp = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.setTimeout(Math.toIntExact(readTimeout.toMillis()));
            session.connect(Math.toIntExact(connectTimeout.toMillis()));

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(Math.toIntExact(connectTimeout.toMillis()));

            return operation.run(channelSftp);
        } catch (JSchException | SftpException | IOException e) {
            throw new ResourceDownloadException(failureMessage, e);
        } finally {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private static byte[] read(ChannelSftp channel, String path) throws SftpException, IOException {
        try (InputStream inputStream = channel.get(path)) {
            return inputStream.readAllBytes();
        }
    }

    private static List<RemoteFile> list(ChannelSftp channel) throws SftpException {
        List<RemoteFile> files = new ArrayList<>();
        for (Object entry : channel.ls(".")) {
            ChannelSftp.LsEntry file = (ChannelSftp.LsEntry) entry;
            files.add(new RemoteFile(file.getFilename(), Integer.toUnsignedLong(file.getAttrs().getMTime())));
        }
        return files;
    }

    @FunctionalInterface
    private interface SftpOperation<T> {
        T run(ChannelSftp channel) throws SftpException, IOException;
    }
}
