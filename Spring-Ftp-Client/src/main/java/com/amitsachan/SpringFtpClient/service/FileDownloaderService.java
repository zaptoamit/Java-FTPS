package com.amitsachan.SpringFtpClient.service;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

public interface FileDownloaderService {
    public InputStream getRemoteServerFileStream(String path);
    public void closeFtpFileWriter();
}
