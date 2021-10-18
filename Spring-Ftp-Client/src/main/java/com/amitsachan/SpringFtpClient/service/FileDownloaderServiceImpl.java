package com.amitsachan.SpringFtpClient.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

@Service
public class FileDownloaderServiceImpl implements FileDownloaderService{
    @Autowired
    FTPFileWriterImpl ftpFileWriter;

    @Override
    public InputStream getRemoteServerFileStream(String path) {
        try {
            ftpFileWriter.open();
            return ftpFileWriter.getInputStreamOfRemoteFile(path);

        }
        catch (GeneralSecurityException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void closeFtpFileWriter(){
        ftpFileWriter.close();
    }
}
