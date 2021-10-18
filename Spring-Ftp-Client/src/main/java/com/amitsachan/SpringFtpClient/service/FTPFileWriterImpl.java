package com.amitsachan.SpringFtpClient.service;

import com.amitsachan.SpringFtpClient.controller.FTPProperties;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.KeyManagerUtils;
import org.apache.commons.net.util.TrustManagerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManager;
import java.io.*;
import java.security.GeneralSecurityException;

@Component
public class FTPFileWriterImpl implements FTPFileWriter {

    private static final Logger logger = LoggerFactory.getLogger(FTPFileWriterImpl.class);
    private com.amitsachan.SpringFtpClient.controller.FTPProperties FTPProperties;
    protected FTPSClient ftpsClient;
    private KeyManager keyManager;

    @Autowired
    public FTPFileWriterImpl(@Autowired FTPProperties FTPProperties) {
        this.FTPProperties = FTPProperties;
    }

    public boolean open() throws GeneralSecurityException, IOException {
        close();
        logger.debug("Connecting and logging in to FTP server.");
        ftpsClient = new FTPSClient("ssl",true);
        ftpsClient.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
        keyManager = KeyManagerUtils.createClientKeyManager(new File("src/main/resources/serverkeystore.p12"), "password");
        ftpsClient.setKeyManager(keyManager);

        boolean loggedIn = false;
        try {
            ftpsClient.connect(FTPProperties.getServer(), FTPProperties.getPort());
            ftpsClient.enterLocalPassiveMode();
            loggedIn = ftpsClient.login(FTPProperties.getUsername(), FTPProperties.getPassword());
            if (FTPProperties.getKeepAliveTimeout() > 0)
                ftpsClient.setControlKeepAliveTimeout(FTPProperties.getKeepAliveTimeout());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return loggedIn;
    }

    public void close() {
        if (ftpsClient != null) {
            try {
                ftpsClient.logout();
                ftpsClient.disconnect();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public boolean loadFile(String remotePath, OutputStream outputStream) {
        try {
            ftpsClient.execPBSZ(0);
            ftpsClient.execPROT("P");
            ftpsClient.setFileType(ftpsClient.BINARY_FILE_TYPE);
            logger.debug("Trying to retrieve a file from remote path " + remotePath);
            return ftpsClient.retrieveFile(remotePath, outputStream);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    public InputStream getInputStreamOfRemoteFile(String remotePath) {
        try {
            ftpsClient.execPBSZ(0);
            ftpsClient.execPROT("P");
            ftpsClient.setFileType(ftpsClient.BINARY_FILE_TYPE);
            logger.debug("Returing input stream from remote path " + remotePath);
            return ftpsClient.retrieveFileStream(remotePath);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return InputStream.nullInputStream();
        }
    }

    public boolean saveFile(InputStream inputStream, String destPath, boolean append) {
        try {
            logger.debug("Trying to store a file to destination path " + destPath);
            if(append)
                return ftpsClient.appendFile(destPath, inputStream);
            else
                return ftpsClient.storeFile(destPath, inputStream);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean saveFile(String sourcePath, String destPath, boolean append) {
        InputStream inputStream = null;
        try {
            inputStream = new ClassPathResource(sourcePath).getInputStream();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        return this.saveFile(inputStream, destPath, append);
    }

    public boolean isConnected() {
        boolean connected = false;
        if (ftpsClient != null) {
            try {
                connected = ftpsClient.sendNoOp();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        logger.debug("Checking for connection to FTP server. Is connected: " + connected);
        return connected;
    }
}